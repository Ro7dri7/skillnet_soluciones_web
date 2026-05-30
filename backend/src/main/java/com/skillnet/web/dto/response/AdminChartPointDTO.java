package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminChartPointDTO {

    private String label;
    private double current;
    private double previous;
}
