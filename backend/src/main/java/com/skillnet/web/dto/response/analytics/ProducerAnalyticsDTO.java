package com.skillnet.web.dto.response.analytics;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProducerAnalyticsDTO {

    private KpiDTO kpis;
    private List<DailyRevenueDTO> revenueTrend = new ArrayList<>();
    private List<CategorySalesDTO> salesByCategory = new ArrayList<>();
    private List<TopCourseDTO> topCourses = new ArrayList<>();
    private List<RecentTransactionDTO> recentTransactions = new ArrayList<>();
}
