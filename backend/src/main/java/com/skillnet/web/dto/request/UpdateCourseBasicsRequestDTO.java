package com.skillnet.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCourseBasicsRequestDTO {

    @Size(max = 255)
    private String title;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "price must be zero or greater")
    private BigDecimal price;

    @Size(max = 200, message = "imageUrl must be at most 200 characters")
    private String imageUrl;

    @Size(max = 500, message = "videoUrl must be at most 500 characters")
    private String videoUrl;

    @Size(max = 10)
    private String language;

    @Size(max = 20)
    private String level;

    @Size(max = 100)
    private String category;

    @Size(max = 200)
    private String subcategory;

    private String whatYouWillLearn;

    private String targetAudience;
}
