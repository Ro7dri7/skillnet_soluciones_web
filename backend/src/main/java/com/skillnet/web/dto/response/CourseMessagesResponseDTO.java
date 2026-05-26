package com.skillnet.web.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseMessagesResponseDTO {

    private Long id;
    private String welcomeMessage;
    private String congratulationsMessage;
}
