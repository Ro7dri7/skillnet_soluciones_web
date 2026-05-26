package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SwitchRoleRequestDTO {

    @NotBlank
    @Pattern(regexp = "student|infoproductor|admin", message = "role must be student, infoproductor or admin")
    private String role;
}
