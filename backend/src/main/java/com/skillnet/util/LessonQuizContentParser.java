package com.skillnet.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.persistence.entity.core.Lesson;

public final class LessonQuizContentParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LessonQuizContentParser() {}

    public static JsonNode extractQuizData(Lesson lesson) {
        if (lesson == null || lesson.getContent() == null || lesson.getContent().isBlank()) {
            return null;
        }
        try {
            JsonNode payload = MAPPER.readTree(lesson.getContent());
            if (payload.has("quizData") && !payload.get("quizData").isNull()) {
                return payload.get("quizData");
            }
            if (payload.has("blocks") && payload.get("blocks").isArray()) {
                for (JsonNode block : payload.get("blocks")) {
                    JsonNode quizData = block.get("quizData");
                    if (quizData != null && !quizData.isNull() && !quizData.isEmpty()) {
                        return quizData;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
