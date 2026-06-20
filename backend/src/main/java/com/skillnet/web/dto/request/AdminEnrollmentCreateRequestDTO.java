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
public class AdminEnrollmentCreateRequestDTO {

    @NotNull
    private Long userId;

    @NotNull
    private Long courseId;

    @NotBlank
    @Size(max = 20)
    private String enrollmentType = "MANUAL";

    private Instant enrolledAt;
}
