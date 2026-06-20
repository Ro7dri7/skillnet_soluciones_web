package com.skillnet.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TrafficAnalyticsResponseDTO {

    private boolean configured;
    private List<TrafficSeriesPointDTO> pageViews;
    private List<TrafficSeriesPointDTO> uniqueVisitors;
}
