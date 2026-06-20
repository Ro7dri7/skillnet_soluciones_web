package com.skillnet.web.dto.response;



import java.time.Instant;

import java.util.List;

import lombok.Builder;

import lombok.Getter;

import lombok.Setter;



@Getter

@Setter

@Builder

public class QuizSubmissionReviewResponseDTO {



    private Long id;

    private Long quizId;

    private String quizTitle;

    private Long courseId;

    private String courseTitle;

    private Long studentId;

    private String studentName;

    private int score;

    private int passingScore;

    private String reviewStatus;

    private String tutorFeedback;

    private Integer timeTakenSeconds;

    private Instant createdAt;

    private List<QuizAnswerReviewDTO> answers;

}


