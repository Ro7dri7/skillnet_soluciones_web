package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCourseRequestDTO {

    @Size(max = 255)
    private String title;

    @Size(max = 30)
    private String courseFormat = "course";

    @Size(max = 100)
    private String category;

    @Size(max = 200)
    private String subcategory;

    private String whatYouWillLearn;

    private String targetAudience;
}
