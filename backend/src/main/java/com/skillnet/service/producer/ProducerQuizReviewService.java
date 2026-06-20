package com.skillnet.service.producer;

import com.skillnet.persistence.entity.core.Answer;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Question;
import com.skillnet.persistence.entity.core.QuizSubmission;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.AnswerRepository;
import com.skillnet.persistence.repository.QuizSubmissionRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.notification.NotificationPublisher;
import com.skillnet.web.dto.request.GradeQuizAnswerRequestDTO;
import com.skillnet.web.dto.request.QuizReviewRequestDTO;
import com.skillnet.web.dto.response.GradeQuizAnswerResponseDTO;
import com.skillnet.web.dto.response.QuizAnswerReviewDTO;
import com.skillnet.web.dto.response.QuizSubmissionReviewResponseDTO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProducerQuizReviewService {

    private final QuizSubmissionRepository quizSubmissionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuizScoreCalculator quizScoreCalculator;
    private final NotificationPublisher notificationPublisher;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public List<QuizSubmissionReviewResponseDTO> listPending(Long professorId) {
        return quizSubmissionRepository.findByQuiz_Course_Professor_IdOrderByCreatedAtDesc(professorId).stream()
                .filter(submission -> "pending".equals(submission.getReviewStatus()))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizSubmissionReviewResponseDTO getSubmission(Long professorId, Long submissionId) {
        QuizSubmission submission = loadOwnedSubmission(professorId, submissionId);
        return toDetail(submission);
    }

    @Transactional
    public QuizSubmissionReviewResponseDTO reviewSubmission(
            Long professorId, Long submissionId, QuizReviewRequestDTO dto) {
        QuizSubmission submission = loadOwnedSubmission(professorId, submissionId);

        User reviewer = userRepository.findById(professorId).orElseThrow();
        int recalculated = quizScoreCalculator.calculateScorePercent(submissionId, submission.getQuiz());
        if (recalculated > 0) {
            submission.setScore(recalculated);
        }

        submission.setReviewStatus(dto.isApproved() ? "approved" : "rejected");
        submission.setTutorFeedback(dto.getFeedback());
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(reviewer);

        QuizSubmission saved = quizSubmissionRepository.save(submission);
        notifyStudentReview(saved, dto.isApproved());

        return toDetail(saved);
    }

    @Transactional
    public GradeQuizAnswerResponseDTO gradeAnswer(
            Long professorId, Long submissionId, GradeQuizAnswerRequestDTO dto) {
        QuizSubmission submission = loadOwnedSubmission(professorId, submissionId);
        Answer answer = answerRepository
                .findByIdAndSubmission_Id(dto.getAnswerId(), submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respuesta no encontrada"));

        answer.setCorrect(dto.getCorrect());
        answer.setTutorFeedback(dto.getFeedback());
        answerRepository.save(answer);

        var quiz = submission.getQuiz();
        int newScore = quizScoreCalculator.calculateScorePercent(submissionId, quiz);
        submission.setScore(newScore);

        if (quizScoreCalculator.allOpenAnswersGraded(submissionId)) {
            if (newScore >= quiz.getPassingScore()) {
                submission.setReviewStatus("approved");
            } else {
                submission.setReviewStatus("rejected");
            }
            User reviewer = userRepository.findById(professorId).orElseThrow();
            submission.setReviewedBy(reviewer);
            submission.setReviewedAt(Instant.now());
            notifyStudentReview(submission, "approved".equals(submission.getReviewStatus()));
        }

        quizSubmissionRepository.save(submission);

        return GradeQuizAnswerResponseDTO.builder()
                .answerId(answer.getId())
                .correct(answer.getCorrect())
                .newScore(newScore)
                .reviewStatus(submission.getReviewStatus())
                .message("Respuesta calificada")
                .build();
    }

    private QuizSubmission loadOwnedSubmission(Long professorId, Long submissionId) {
        QuizSubmission submission = quizSubmissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envío no encontrado"));

        Long ownerId = submission.getQuiz().getCourse().getProfessor().getId();
        if (!ownerId.equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
        return submission;
    }

    private void notifyStudentReview(QuizSubmission submission, boolean approved) {
        User student = submission.getUser();
        Course course = submission.getQuiz().getCourse();
        String title = approved ? "Quiz aprobado" : "Quiz revisado";
        String message = approved
                ? "Tu envío en \"" + submission.getQuiz().getTitle() + "\" fue aprobado."
                : "Tu envío en \"" + submission.getQuiz().getTitle() + "\" fue revisado.";
        notificationPublisher.publish(
                student,
                "quiz_review",
                title,
                message,
                frontendBaseUrl + "/marketplace/course/" + course.getSlug() + "/learn");
    }

    private QuizSubmissionReviewResponseDTO toSummary(QuizSubmission submission) {
        User student = submission.getUser();
        String studentName = formatName(student);

        return QuizSubmissionReviewResponseDTO.builder()
                .id(submission.getId())
                .quizId(submission.getQuiz().getId())
                .quizTitle(submission.getQuiz().getTitle())
                .courseId(submission.getQuiz().getCourse().getId())
                .courseTitle(submission.getQuiz().getCourse().getTitle())
                .studentId(student.getId())
                .studentName(studentName)
                .score(submission.getScore())
                .reviewStatus(submission.getReviewStatus())
                .passingScore(submission.getQuiz().getPassingScore())
                .timeTakenSeconds(submission.getTimeTakenSeconds())
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private QuizSubmissionReviewResponseDTO toDetail(QuizSubmission submission) {
        List<QuizAnswerReviewDTO> answers = answerRepository.findBySubmissionIdWithQuestion(submission.getId()).stream()
                .map(this::toAnswerDto)
                .toList();

        QuizSubmissionReviewResponseDTO summary = toSummary(submission);
        return QuizSubmissionReviewResponseDTO.builder()
                .id(summary.getId())
                .quizId(summary.getQuizId())
                .quizTitle(summary.getQuizTitle())
                .courseId(summary.getCourseId())
                .courseTitle(summary.getCourseTitle())
                .studentId(summary.getStudentId())
                .studentName(summary.getStudentName())
                .score(summary.getScore())
                .reviewStatus(summary.getReviewStatus())
                .passingScore(summary.getPassingScore())
                .tutorFeedback(submission.getTutorFeedback())
                .timeTakenSeconds(summary.getTimeTakenSeconds())
                .createdAt(summary.getCreatedAt())
                .answers(answers)
                .build();
    }

    private QuizAnswerReviewDTO toAnswerDto(Answer answer) {
        Question question = answer.getQuestion();
        String questionType = question != null ? question.getQuestionType() : "";
        String textAnswer = answer.getTextAnswer();
        if (textAnswer == null && answer.getChoice() != null) {
            textAnswer = answer.getChoice().getText();
        }
        if (textAnswer == null && answer.getBooleanAnswer() != null) {
            textAnswer = Boolean.TRUE.equals(answer.getBooleanAnswer()) ? "Verdadero" : "Falso";
        }

        return QuizAnswerReviewDTO.builder()
                .id(answer.getId())
                .questionId(question != null ? question.getId() : null)
                .questionText(question != null ? question.getText() : "")
                .questionType(questionType)
                .textAnswer(textAnswer)
                .correct(answer.getCorrect())
                .tutorFeedback(answer.getTutorFeedback())
                .requiresManualGrading(quizScoreCalculator.isOpenType(questionType))
                .build();
    }

    private String formatName(User student) {
        String studentName = ((student.getFirstName() != null ? student.getFirstName() : "")
                        + " "
                        + (student.getLastName() != null ? student.getLastName() : ""))
                .trim();
        return studentName.isBlank() ? student.getUsername() : studentName;
    }
}
