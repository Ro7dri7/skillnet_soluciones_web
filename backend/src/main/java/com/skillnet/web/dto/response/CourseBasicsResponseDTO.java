package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseBasicsResponseDTO {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String videoUrl;
    private String language;
    private String level;
    private String category;
    private String subcategory;
    private String status;
}
