package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GammaGenerateRequestDTO {
    @NotBlank
    private String prompt;

    private Integer pages;
    private String format;
    private Long courseId;
    private Long lessonId;

    /** Título explícito del documento en Gamma. */
    private String title;

    /** Código ISO de idioma (es, en, pt, …). */
    private String language;

    private String tone;
    private String audience;

    /** brief | medium | detailed | extensive */
    private String textAmount;

    /** aiGenerated | noImages | webFreeToUseCommercially */
    private String imageSource;

    /** Estilo visual cuando imageSource=aiGenerated. */
    private String imageStyle;

    private String additionalInstructions;

    /** Texto extraído de un PDF de referencia subido por el usuario. */
    private String sourceMaterial;
}
