package com.skillnet.service.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.persistence.entity.core.Choice;
import com.skillnet.persistence.entity.core.Question;
import com.skillnet.persistence.entity.core.Quiz;
import com.skillnet.persistence.repository.ChoiceRepository;
import com.skillnet.persistence.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizQuestionSyncService {

    private static final JsonNode EMPTY_OBJECT = JsonNodeFactory.instance.objectNode();
    private static final JsonNode EMPTY_ARRAY = JsonNodeFactory.instance.arrayNode();

    private final QuestionRepository questionRepository;
    private final ChoiceRepository choiceRepository;

    @Transactional
    public List<Question> syncFromQuizData(Quiz quiz, JsonNode quizData) {
        if (quizData == null || quizData.isNull()) {
            return questionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        }

        JsonNode questionsNode = quizData.get("questions");
        if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
            return questionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        }

        List<Question> existing = questionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        if (existing.size() == questionsNode.size()) {
            return existing;
        }

        List<Question> synced = new ArrayList<>();
        for (int i = 0; i < questionsNode.size(); i++) {
            JsonNode qNode = questionsNode.get(i);
            Question question = new Question();
            question.setQuiz(quiz);
            question.setText(qNode.path("text").asText("Pregunta " + (i + 1)));
            question.setQuestionType(mapQuestionType(qNode.path("type").asText("multiple")));
            question.setMatchingPairs(EMPTY_ARRAY);
            question.setOrderingItems(EMPTY_ARRAY);
            question.setFillBlankData(EMPTY_OBJECT);
            question.setDragDropData(EMPTY_OBJECT);
            question = questionRepository.save(question);

            String type = qNode.path("type").asText("multiple");
            if ("multiple".equalsIgnoreCase(type) || "true_false".equalsIgnoreCase(type)) {
                JsonNode options = qNode.get("options");
                int correctIndex = qNode.path("correctIndex").asInt(0);
                if (options != null && options.isArray()) {
                    for (int j = 0; j < options.size(); j++) {
                        Choice choice = new Choice();
                        choice.setQuestion(question);
                        choice.setText(options.get(j).asText("Opción " + (j + 1)));
                        choice.setCorrect(j == correctIndex);
                        choiceRepository.save(choice);
                    }
                }
            }

            synced.add(question);
        }
        return synced;
    }

    private String mapQuestionType(String uiType) {
        if (uiType == null) {
            return "single_choice";
        }
        return switch (uiType.toLowerCase(Locale.ROOT)) {
            case "true_false" -> "true_false";
            case "matching" -> "matching";
            case "free_response", "free_text", "text", "open_text" -> "free_text";
            default -> "single_choice";
        };
    }
}
