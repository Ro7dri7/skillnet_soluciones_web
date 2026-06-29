package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TwoFactorDisableRequestDTO {

    /** Opcional para cuentas creadas con Google; obligatorio si tienes contraseña local. */
    private String password;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String code;
}
