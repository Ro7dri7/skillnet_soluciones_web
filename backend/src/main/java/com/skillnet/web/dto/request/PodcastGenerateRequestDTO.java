package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PodcastGenerateRequestDTO {
    private String topic;
    private String text;
    private Long courseId;
    private Long lessonId;
    private boolean transcriptOnly;
    /** Código ISO corto: es, en, pt */
    private String language;
    /** Duración objetivo del episodio en minutos (1–10). */
    private Integer durationMinutes;
}
