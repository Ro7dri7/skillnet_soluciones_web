package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReferralLinkResponseDTO {

    private Long courseId;
    private String token;
    private String url;
}
