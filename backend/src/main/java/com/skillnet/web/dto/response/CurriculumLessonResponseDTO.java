package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurriculumLessonResponseDTO {

    private Long id;
    private String title;
    /** Tipo UI: text, image, video, pdf, quiz, audio */
    private String contentType;
    private String resourceUrl;
    private String textContent;
    private JsonNode quizData;
    private int orderIndex;
    /** Bloques de contenido apilados dentro de la lección. */
    private JsonNode blocks;
}
