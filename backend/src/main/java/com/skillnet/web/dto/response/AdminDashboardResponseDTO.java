package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminDashboardResponseDTO {

    private String periodLabel;
    private String periodStart;
    private String periodEnd;
    private String view;
    @Builder.Default
    private List<AdminKpiDTO> kpis = new ArrayList<>();
    @Builder.Default
    private List<AdminChartPointDTO> revenueSeries = new ArrayList<>();
    @Builder.Default
    private List<AdminChartPointDTO> usersSeries = new ArrayList<>();
    @Builder.Default
    private List<AdminTransactionDTO> recentTransactions = new ArrayList<>();
    @Builder.Default
    private List<AdminTopProducerDTO> topProducers = new ArrayList<>();
    private long pendingDraftCourses;
    private long inactiveUsersTotal;
}
