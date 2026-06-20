package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CourseReviewResponseDTO {

    private Long id;
    private Long courseId;
    private Long userId;
    private UserSummaryDTO user;
    private BigDecimal rating;
    private String comment;
    private int helpfulCount;
    private Instant createdAt;
    private Instant updatedAt;
}
