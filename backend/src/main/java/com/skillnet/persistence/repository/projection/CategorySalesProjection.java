package com.skillnet.persistence.repository.projection;

import java.math.BigDecimal;

public interface CategorySalesProjection {

    String getCategoryName();

    BigDecimal getTotalSales();
}
