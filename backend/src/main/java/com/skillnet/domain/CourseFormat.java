package com.skillnet.domain;

/**
 * Valores de {@code core_course.course_format} usados en el Course Builder.
 */
public enum CourseFormat {
    VIDEOCOURSE("course"),
    EBOOK("ebook"),
    AUDIOBOOK("audiobook"),
    PODCAST("podcast"),
    WORKSHOP("workshop"),
    SUBSCRIPTION("subscription"),
    EVENT("event"),
    APP("app"),
    SCRIPT("script"),
    IMAGE("image");

    private final String dbValue;

    CourseFormat(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CourseFormat fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            return VIDEOCOURSE;
        }
        String normalized = value.trim().toLowerCase();
        for (CourseFormat format : values()) {
            if (format.dbValue.equals(normalized) || format.name().equalsIgnoreCase(normalized)) {
                return format;
            }
        }
        return VIDEOCOURSE;
    }
}
