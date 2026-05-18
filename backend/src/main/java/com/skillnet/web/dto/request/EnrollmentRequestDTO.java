package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnrollmentRequestDTO {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "courseId is required")
    private Long courseId;

    @NotNull(message = "enrolledAt is required")
    private Instant enrolledAt;

    @NotBlank(message = "enrollmentType is required")
    @Size(max = 20)
    private String enrollmentType;

    private Long enrolledById;
    private boolean completed;
    private Instant completedAt;
}
