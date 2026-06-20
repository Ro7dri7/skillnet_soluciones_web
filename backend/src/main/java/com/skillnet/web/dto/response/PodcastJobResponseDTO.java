package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PodcastJobResponseDTO {
    private Long jobId;
    private String status;
    private String transcript;
    private String audioUrl;
    private boolean transcriptOnly;
    private String language;
    private Integer durationMinutes;
    private String errorMessage;
}
