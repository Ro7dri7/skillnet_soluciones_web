package com.skillnet.service.student;



import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.skillnet.api.dto.student.QuizAnswerSubmissionDTO;

import com.skillnet.api.dto.student.QuizAttemptsResponseDTO;

import com.skillnet.api.dto.student.QuizSubmissionRequestDTO;

import com.skillnet.api.dto.student.QuizSubmissionResponseDTO;

import com.skillnet.persistence.entity.core.Answer;

import com.skillnet.persistence.entity.core.Choice;

import com.skillnet.persistence.entity.core.Course;

import com.skillnet.persistence.entity.core.Lesson;

import com.skillnet.persistence.entity.core.Question;

import com.skillnet.persistence.entity.core.Quiz;

import com.skillnet.persistence.entity.core.QuizSubmission;

import com.skillnet.persistence.entity.core.User;

import com.skillnet.persistence.repository.AnswerRepository;

import com.skillnet.persistence.repository.ChoiceRepository;

import com.skillnet.persistence.repository.EnrollmentRepository;

import com.skillnet.persistence.repository.LessonRepository;

import com.skillnet.persistence.repository.QuizRepository;

import com.skillnet.persistence.repository.QuizSubmissionRepository;

import com.skillnet.persistence.repository.UserRepository;

import com.skillnet.service.producer.QuizScoreCalculator;

import com.skillnet.util.LessonQuizContentParser;

import java.time.Instant;

import java.util.List;

import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;



@Service

@RequiredArgsConstructor

public class StudentQuizService {



    private static final JsonNode EMPTY_OBJECT = JsonNodeFactory.instance.objectNode();

    private static final JsonNode EMPTY_ARRAY = JsonNodeFactory.instance.arrayNode();



    private final LessonRepository lessonRepository;

    private final QuizRepository quizRepository;

    private final QuizSubmissionRepository quizSubmissionRepository;

    private final EnrollmentRepository enrollmentRepository;

    private final UserRepository userRepository;

    private final AnswerRepository answerRepository;

    private final ChoiceRepository choiceRepository;

    private final QuizQuestionSyncService quizQuestionSyncService;

    private final QuizScoreCalculator quizScoreCalculator;



    @Transactional

    public QuizSubmissionResponseDTO submitQuiz(

            Long lessonId, Long userId, QuizSubmissionRequestDTO dto) {

        Lesson lesson = lessonRepository

                .findByIdWithCourse(lessonId)

                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));



        Course course = lesson.getCourse();

        if (!enrollmentRepository.existsByUser_IdAndCourse_Id(userId, course.getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No estás inscrito en este curso");

        }



        JsonNode quizData = LessonQuizContentParser.extractQuizData(lesson);

        Quiz quiz = findOrCreateQuiz(course, lesson, quizData);

        long attemptsUsed = quizSubmissionRepository.countByUser_IdAndQuiz_Id(userId, quiz.getId());

        if (attemptsUsed >= quiz.getMaxAttempts()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Has agotado los intentos disponibles");

        }



        User user = userRepository.findById(userId).orElseThrow();

        List<Question> questions = quizQuestionSyncService.syncFromQuizData(quiz, quizData);

        boolean needsManualReview = dto.isNeedsManualReview()

                || questions.stream().anyMatch(q -> quizScoreCalculator.isOpenType(q.getQuestionType()));



        QuizSubmission submission = new QuizSubmission();

        submission.setUser(user);

        submission.setQuiz(quiz);

        submission.setScore(dto.getScore());

        submission.setReviewStatus(needsManualReview ? "pending" : "approved");

        submission.setTimeTakenSeconds(dto.getTimeTakenSeconds());

        submission.setCreatedAt(Instant.now());



        QuizSubmission saved = quizSubmissionRepository.save(submission);

        persistAnswers(saved, questions, dto.getAnswers(), quizData);



        int calculatedScore = quizScoreCalculator.calculateScorePercent(saved.getId(), quiz);

        if (calculatedScore > 0 || !needsManualReview) {

            saved.setScore(calculatedScore > 0 ? calculatedScore : dto.getScore());

        }

        if (!needsManualReview) {

            if (saved.getScore() < quiz.getPassingScore()) {

                saved.setReviewStatus("rejected");

            } else {

                saved.setReviewStatus("approved");

            }

        }

        saved = quizSubmissionRepository.save(saved);



        return QuizSubmissionResponseDTO.builder()

                .id(saved.getId())

                .quizId(quiz.getId())

                .lessonId(lessonId)

                .score(saved.getScore())

                .reviewStatus(saved.getReviewStatus())

                .timeTakenSeconds(saved.getTimeTakenSeconds())

                .createdAt(saved.getCreatedAt())

                .build();

    }



    @Transactional(readOnly = true)

    public QuizAttemptsResponseDTO getAttempts(Long lessonId, Long userId) {

        Lesson lesson = lessonRepository

                .findByIdWithCourse(lessonId)

                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));



        JsonNode quizData = LessonQuizContentParser.extractQuizData(lesson);

        Quiz quiz = findQuizForLesson(lesson.getCourse(), lesson);

        if (quiz == null) {

            int maxAttempts = quizData != null ? quizData.path("maxAttempts").asInt(3) : 3;

            return QuizAttemptsResponseDTO.builder()

                    .quizId(null)

                    .attemptsUsed(0)

                    .maxAttempts(maxAttempts)

                    .attemptsRemaining(maxAttempts)

                    .canSubmit(true)

                    .build();

        }



        int attemptsUsed = (int) quizSubmissionRepository.countByUser_IdAndQuiz_Id(userId, quiz.getId());

        int maxAttempts = quiz.getMaxAttempts();

        int remaining = Math.max(0, maxAttempts - attemptsUsed);



        return QuizAttemptsResponseDTO.builder()

                .quizId(quiz.getId())

                .attemptsUsed(attemptsUsed)

                .maxAttempts(maxAttempts)

                .attemptsRemaining(remaining)

                .canSubmit(remaining > 0)

                .build();

    }



    private void persistAnswers(

            QuizSubmission submission,

            List<Question> questions,

            List<QuizAnswerSubmissionDTO> answerDtos,

            JsonNode quizData) {

        if (questions.isEmpty()) {

            return;

        }



        JsonNode questionsNode = quizData != null ? quizData.get("questions") : null;

        for (int i = 0; i < questions.size(); i++) {

            Question question = questions.get(i);

            QuizAnswerSubmissionDTO dto = findAnswerDto(answerDtos, questionsNode, i);

            Answer answer = new Answer();

            answer.setSubmission(submission);

            answer.setQuestion(question);

            answer.setMatchingPairs(EMPTY_ARRAY);

            answer.setOrderingAnswer(EMPTY_ARRAY);

            answer.setFillBlankAnswer(EMPTY_OBJECT);

            answer.setDragDropAnswer(EMPTY_OBJECT);



            String type = question.getQuestionType();

            if (quizScoreCalculator.isOpenType(type)) {

                answer.setTextAnswer(dto != null ? dto.getTextAnswer() : null);

                answer.setCorrect(null);

            } else if ("matching".equalsIgnoreCase(type)) {

                ArrayNode pairs = JsonNodeFactory.instance.arrayNode();

                if (dto != null && dto.getMatchingAnswers() != null) {

                    dto.getMatchingAnswers().forEach(pairs::add);

                }

                answer.setMatchingPairs(pairs);

                answer.setCorrect(isMatchingCorrect(questionsNode, i, dto));

            } else if ("true_false".equalsIgnoreCase(type)) {

                Boolean boolVal = dto != null && dto.getOptionIndex() != null ? dto.getOptionIndex() == 0 : null;

                answer.setBooleanAnswer(boolVal);

                answer.setCorrect(isChoiceCorrect(question, dto));

            } else {

                Choice selected = resolveChoice(question, dto);

                answer.setChoice(selected);

                answer.setCorrect(selected != null && selected.isCorrect());

            }



            answerRepository.save(answer);

        }

    }



    private QuizAnswerSubmissionDTO findAnswerDto(

            List<QuizAnswerSubmissionDTO> answerDtos, JsonNode questionsNode, int index) {

        if (answerDtos == null || answerDtos.isEmpty()) {

            return null;

        }

        if (questionsNode != null && questionsNode.isArray() && index < questionsNode.size()) {

            String clientId = questionsNode.get(index).path("id").asText("");

            if (!clientId.isBlank()) {

                for (QuizAnswerSubmissionDTO dto : answerDtos) {

                    if (clientId.equals(dto.getQuestionId())) {

                        return dto;

                    }

                }

            }

        }

        return index < answerDtos.size() ? answerDtos.get(index) : null;

    }



    private Choice resolveChoice(Question question, QuizAnswerSubmissionDTO dto) {

        if (dto == null || dto.getOptionIndex() == null) {

            return null;

        }

        List<Choice> choices = choiceRepository.findByQuestion_IdOrderByIdAsc(question.getId());

        int idx = dto.getOptionIndex();

        if (idx < 0 || idx >= choices.size()) {

            return null;

        }

        return choices.get(idx);

    }



    private Boolean isChoiceCorrect(Question question, QuizAnswerSubmissionDTO dto) {

        Choice choice = resolveChoice(question, dto);

        return choice != null && choice.isCorrect();

    }



    private Boolean isMatchingCorrect(JsonNode questionsNode, int index, QuizAnswerSubmissionDTO dto) {

        if (questionsNode == null || !questionsNode.isArray() || index >= questionsNode.size()) {

            return false;

        }

        JsonNode qNode = questionsNode.get(index);

        JsonNode expected = qNode.get("matches");

        if (expected == null || !expected.isArray() || dto == null || dto.getMatchingAnswers() == null) {

            return false;

        }

        List<String> given = dto.getMatchingAnswers();

        for (int i = 0; i < expected.size(); i++) {

            String exp = expected.get(i).asText("").trim().toLowerCase(Locale.ROOT);

            String got = i < given.size() ? String.valueOf(given.get(i)).trim().toLowerCase(Locale.ROOT) : "";

            if (!exp.equals(got)) {

                return false;

            }

        }

        return true;

    }



    private Quiz findQuizForLesson(Course course, Lesson lesson) {

        return quizRepository.findByCourse_Id(course.getId()).stream()

                .filter(q -> q.getOrderIndex() == lesson.getOrderIndex())

                .filter(q -> {

                    if (q.getSection() == null && lesson.getSection() == null) {

                        return true;

                    }

                    if (q.getSection() == null || lesson.getSection() == null) {

                        return false;

                    }

                    return q.getSection().getId().equals(lesson.getSection().getId());

                })

                .findFirst()

                .orElse(null);

    }



    private Quiz findOrCreateQuiz(Course course, Lesson lesson, JsonNode quizData) {

        Quiz existing = findQuizForLesson(course, lesson);

        if (existing != null) {

            if (quizData != null && !quizData.isNull()) {

                existing.setPassingScore(quizData.path("passingScore").asInt(existing.getPassingScore()));

                existing.setMaxAttempts(quizData.path("maxAttempts").asInt(existing.getMaxAttempts()));

                existing.setTimeLimitMinutes(quizData.path("timeLimitMinutes").asInt(existing.getTimeLimitMinutes()));

                quizRepository.save(existing);

            }

            return existing;

        }



        Quiz quiz = new Quiz();

        quiz.setCourse(course);

        quiz.setSection(lesson.getSection());

        quiz.setTitle(lesson.getTitle());

        quiz.setOrderIndex(lesson.getOrderIndex());

        quiz.setTimeLimitMinutes(quizData != null ? quizData.path("timeLimitMinutes").asInt(30) : 30);

        quiz.setPassingScore(quizData != null ? quizData.path("passingScore").asInt(70) : 70);

        quiz.setMaxAttempts(quizData != null ? quizData.path("maxAttempts").asInt(3) : 3);

        quiz.setShuffleQuestions(false);

        quiz.setShuffleOptions(false);

        quiz.setShowAnswersAtEnd(true);

        quiz.setAllowBackNavigation(true);

        return quizRepository.save(quiz);

    }

}


