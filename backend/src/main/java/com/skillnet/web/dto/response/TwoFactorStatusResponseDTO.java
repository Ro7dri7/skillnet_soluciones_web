package com.skillnet.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorStatusResponseDTO {

    private boolean enabled;
    private String method;
    /** false para cuentas Google (sin contraseña local usable). */
    private boolean passwordRequiredForDisable;
}
