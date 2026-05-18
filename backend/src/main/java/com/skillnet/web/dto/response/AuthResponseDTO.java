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

    public AuthResponseDTO(String token, UserSummaryDTO user) {
        this.token = token;
        this.type = "Bearer";
        this.user = user;
    }
}
