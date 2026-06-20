package com.skillnet.web.dto.response;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DbSchemaHealthResponseDTO {
    private Map<String, Boolean> tablesPresent;
    private Map<String, Long> rowCounts;
    private Map<String, String> notes;
}
