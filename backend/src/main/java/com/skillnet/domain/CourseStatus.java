package com.skillnet.domain;

public enum CourseStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    DELETED("deleted");

    private final String dbValue;

    CourseStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
