package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PodcastSynthesizeRequestDTO {
    @NotBlank
    private String approvedTranscript;
}
