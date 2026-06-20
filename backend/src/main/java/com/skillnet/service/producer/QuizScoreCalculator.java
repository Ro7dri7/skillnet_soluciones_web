package com.skillnet.service.producer;

import com.skillnet.persistence.entity.core.Answer;
import com.skillnet.persistence.entity.core.Question;
import com.skillnet.persistence.entity.core.Quiz;
import com.skillnet.persistence.repository.AnswerRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuizScoreCalculator {

    private static final List<String> OPEN_TYPES = List.of("free_text", "text", "open_text");

    private final AnswerRepository answerRepository;

    public int calculateScorePercent(Long submissionId, Quiz quiz) {
        List<Answer> answers = answerRepository.findBySubmissionIdWithQuestion(submissionId);
        if (answers.isEmpty()) {
            return 0;
        }
        int gradable = 0;
        int correct = 0;
        for (Answer answer : answers) {
            Question question = answer.getQuestion();
            if (question == null) {
                continue;
            }
            if (isOpenType(question.getQuestionType())) {
                if (answer.getCorrect() == null) {
                    continue;
                }
                gradable++;
                if (Boolean.TRUE.equals(answer.getCorrect())) {
                    correct++;
                }
                continue;
            }
            gradable++;
            if (Boolean.TRUE.equals(answer.getCorrect())) {
                correct++;
            } else if (answer.getCorrect() == null && answer.getChoice() != null) {
                if (Boolean.TRUE.equals(answer.getChoice().isCorrect())) {
                    correct++;
                }
            }
        }
        if (gradable == 0) {
            return 0;
        }
        return (int) Math.round((correct * 100.0) / gradable);
    }

    public boolean allOpenAnswersGraded(Long submissionId) {
        List<Answer> answers = answerRepository.findBySubmissionIdWithQuestion(submissionId);
        for (Answer answer : answers) {
            Question question = answer.getQuestion();
            if (question != null && isOpenType(question.getQuestionType()) && answer.getCorrect() == null) {
                return false;
            }
        }
        return true;
    }

    public boolean isOpenType(String questionType) {
        if (questionType == null) {
            return false;
        }
        return OPEN_TYPES.contains(questionType.toLowerCase(Locale.ROOT));
    }
}
