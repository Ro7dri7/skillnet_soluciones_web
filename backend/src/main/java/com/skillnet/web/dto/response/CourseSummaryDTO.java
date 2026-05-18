package com.skillnet.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryDTO {

    private Long id;
    private String title;
    private String slug;
    private String status;
}
