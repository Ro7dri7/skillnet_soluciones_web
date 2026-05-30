package com.skillnet.web.dto.response.analytics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningCourseDTO {

    private Long id;
    private String title;
    private String professor;
    private String category;
    private String slug;
    private String thumbnailUrl;
    private int progress;
    private int lessonsDone;
    private int lessonsTotal;
    private String enrolledAt;
}
