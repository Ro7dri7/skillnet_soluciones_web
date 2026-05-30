package com.skillnet.service.student;

import com.skillnet.web.dto.response.analytics.StudentAnalyticsDTO;

public interface StudentAnalyticsService {

    StudentAnalyticsDTO getAnalytics(Long userId, Integer year, Integer month);
}
