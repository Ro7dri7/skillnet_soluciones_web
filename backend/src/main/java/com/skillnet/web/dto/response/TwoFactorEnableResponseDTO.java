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
public class TwoFactorEnableResponseDTO {

    private String method;
    /** Secreto Base32 (manual entry en la app). */
    private String secret;
    /** Data URI PNG para escanear con Google Authenticator / Authy. */
    private String qrCode;
}
