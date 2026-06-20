package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CertificateItemDTO {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private String certificateFile;
    private Instant uploadedAt;
}
