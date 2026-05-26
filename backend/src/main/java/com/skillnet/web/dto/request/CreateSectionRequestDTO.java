package com.skillnet.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateSectionRequestDTO {

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Min(0)
    private int orderIndex;
}
