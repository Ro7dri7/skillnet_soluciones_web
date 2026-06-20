package com.skillnet.service.producer;

import com.skillnet.web.dto.response.TrafficAnalyticsResponseDTO;
import com.skillnet.web.dto.response.TrafficSeriesPointDTO;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrafficAnalyticsService {

    @Value("${skillnet.google-analytics.property-id:}")
    private String gaPropertyId;

    public TrafficAnalyticsResponseDTO getTrafficAnalytics(Long professorId) {
        boolean configured = gaPropertyId != null && !gaPropertyId.isBlank();
        List<TrafficSeriesPointDTO> emptySeries = Collections.emptyList();

        return TrafficAnalyticsResponseDTO.builder()
                .configured(configured)
                .pageViews(emptySeries)
                .uniqueVisitors(emptySeries)
                .build();
    }
}
