package com.skillnet.web.dto.response.analytics;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KpiDTO {

    private BigDecimal totalRevenue;
    private Long activeStudents;
    private Integer publishedCourses;
    /** Ventas completadas (pagos) en el periodo seleccionado. */
    private Long coursesSold;
    private Double avgRating;
}
