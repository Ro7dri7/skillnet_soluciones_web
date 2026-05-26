package com.skillnet.domain;

/**
 * Tipos de lección del builder. Se persisten en {@code core_lesson.content_type}.
 */
public enum LessonType {
    VIDEO("lesson"),
    TEXT("lesson"),
    QUIZ("quiz");

    private final String contentType;

    LessonType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static LessonType fromContentType(String value) {
        if (value == null || value.isBlank()) {
            return VIDEO;
        }
        String normalized = value.trim().toLowerCase();
        if ("quiz".equals(normalized)) {
            return QUIZ;
        }
        if ("text".equals(normalized)) {
            return TEXT;
        }
        return VIDEO;
    }
}
