package com.skillnet.web.dto.response.analytics;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TopCourseDTO {

    private Long id;
    private String title;
    private Long studentsCount;
    private BigDecimal revenue;
}
