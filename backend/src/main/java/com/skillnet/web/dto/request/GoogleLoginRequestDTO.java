package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleLoginRequestDTO {

    @NotBlank(message = "token is required")
    private String token;

    /** Vista activa tras registro/login (student | infoproductor). */
    private String activeRole;
}
