package com.skillnet.persistence.repository.projection;

import java.math.BigDecimal;

public interface CourseRevenueProjection {

    Long getCourseId();

    BigDecimal getRevenue();
}
