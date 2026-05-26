package com.skillnet.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Actualización parcial de sección (título y/o orden). */
@Getter
@Setter
@NoArgsConstructor
public class SectionUpdateRequestDTO {

    @Size(max = 255)
    private String title;

    private Integer orderIndex;
}
