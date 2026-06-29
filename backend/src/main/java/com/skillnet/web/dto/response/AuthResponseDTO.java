package com.skillnet.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String type = "Bearer";
    private UserSummaryDTO user;
    /** true cuando falta verificar el correo; token será null hasta confirmar el código. */
    private boolean verificationRequired;
    /** true cuando el login requiere código TOTP antes de emitir JWT. */
    private boolean twoFactorRequired;
    private String twoFactorToken;
    private String twoFactorMethod;
    private String message;

    public AuthResponseDTO(String token, UserSummaryDTO user) {
        this.token = token;
        this.type = "Bearer";
        this.user = user;
    }

    public static AuthResponseDTO verificationPending(UserSummaryDTO user, String message) {
        AuthResponseDTO dto = new AuthResponseDTO(null, user);
        dto.setVerificationRequired(true);
        dto.setMessage(message);
        return dto;
    }

    public static AuthResponseDTO twoFactorPending(
            UserSummaryDTO user, String twoFactorToken, String method) {
        AuthResponseDTO dto = new AuthResponseDTO(null, user);
        dto.setTwoFactorRequired(true);
        dto.setTwoFactorToken(twoFactorToken);
        dto.setTwoFactorMethod(method);
        dto.setMessage("Introduce el código de tu app autenticadora.");
        return dto;
    }
}
