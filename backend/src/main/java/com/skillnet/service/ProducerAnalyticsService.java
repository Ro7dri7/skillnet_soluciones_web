package com.skillnet.service;

import com.skillnet.web.dto.response.analytics.ProducerAnalyticsDTO;

public interface ProducerAnalyticsService {

    ProducerAnalyticsDTO getAnalytics(Long professorId, Integer year, Integer month);
}
