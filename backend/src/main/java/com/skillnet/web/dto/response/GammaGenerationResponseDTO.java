package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GammaGenerationResponseDTO {
    private String id;
    private String status;
    private String gammaUrl;
    private String exportUrl;
    /** URL del PDF servido por SkillNet (priorizar en el front). */
    private String platformExportUrl;
    private JsonNode raw;
}
