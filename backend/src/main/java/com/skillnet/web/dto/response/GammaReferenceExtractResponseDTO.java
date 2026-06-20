package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GammaReferenceExtractResponseDTO {
    private String extractedText;
    private int characterCount;
}
