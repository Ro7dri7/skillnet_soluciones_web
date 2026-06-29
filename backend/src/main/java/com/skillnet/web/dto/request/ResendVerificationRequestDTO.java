package com.skillnet.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequestDTO {

    @NotBlank
    @Email
    private String email;
}
