package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CertificateCheckResponseDTO {

    private Long courseId;
    private boolean hasCertificate;
    private boolean eligible;
    private String message;
    private CertificateItemDTO certificate;
}
