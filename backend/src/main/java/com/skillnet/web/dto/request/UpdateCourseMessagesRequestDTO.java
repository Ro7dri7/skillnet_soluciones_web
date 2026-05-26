package com.skillnet.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCourseMessagesRequestDTO {

    @Size(max = 1000)
    private String welcomeMessage;

    @Size(max = 1000)
    private String congratulationsMessage;
}
