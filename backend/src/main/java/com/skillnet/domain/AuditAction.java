package com.skillnet.domain;

public final class AuditAction {

    public static final String CREATE_COURSE = "CREATE_COURSE";
    public static final String UPDATE_COURSE = "UPDATE_COURSE";
    public static final String DELETE_COURSE = "DELETE_COURSE";
    public static final String PUBLISH_COURSE = "PUBLISH_COURSE";
    public static final String UNPUBLISH_COURSE = "UNPUBLISH_COURSE";
    public static final String TAKEDOWN_COURSE = "TAKEDOWN_COURSE";
    public static final String SET_DRAFT_COURSE = "SET_DRAFT";
    public static final String PURCHASE_COURSE = "PURCHASE_COURSE";
    public static final String PURCHASE_PLAN = "PURCHASE_PLAN";
    public static final String UPDATE_PROFILE = "UPDATE_PROFILE";
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String REGISTER_USER = "REGISTER_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String SWITCH_ROLE = "SWITCH_ROLE";
    public static final String ADMIN_ENROLL_USER = "ADMIN_ENROLL_USER";
    public static final String ADMIN_CHANGE_USER_ROLE = "ADMIN_CHANGE_USER_ROLE";
    public static final String ENABLE_2FA = "ENABLE_2FA";
    public static final String DISABLE_2FA = "DISABLE_2FA";
    public static final String LOGIN_2FA = "LOGIN_2FA";

    public static final String ENTITY_COURSE = "Course";
    public static final String ENTITY_PAYMENT = "Payment";
    public static final String ENTITY_ENROLLMENT = "Enrollment";
    public static final String ENTITY_USER = "User";
    public static final String ENTITY_SERVICE_OFFERING = "ServiceOffering";

    private AuditAction() {}
}
