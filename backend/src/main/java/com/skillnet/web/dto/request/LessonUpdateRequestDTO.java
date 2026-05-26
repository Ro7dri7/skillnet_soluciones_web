package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Actualización parcial de lección (contenido, quiz, orden). */
@Getter
@Setter
@NoArgsConstructor
public class LessonUpdateRequestDTO {

    @Size(max = 255)
    private String title;

    @Size(max = 20)
    private String contentType;

    @Size(max = 1000)
    private String resourceUrl;

    private String textContent;

    private JsonNode quizData;

    private Integer orderIndex;

    /** Bloques de contenido dentro de la lección (JSON array). */
    private JsonNode blocks;
}
