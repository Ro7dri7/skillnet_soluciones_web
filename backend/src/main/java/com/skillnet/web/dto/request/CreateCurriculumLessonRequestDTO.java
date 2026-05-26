package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCurriculumLessonRequestDTO {

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "contentType is required")
    @Size(max = 20)
    private String contentType;

    @Size(max = 1000)
    private String resourceUrl;

    private String textContent;

    private JsonNode quizData;

    /** Bloques de contenido; array vacío = lección sin bloques aún. */
    private JsonNode blocks;
}
