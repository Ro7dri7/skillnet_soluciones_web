package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminKpiDTO {

    private String id;
    private String label;
    private String value;
    private String changePercent;
    private String changeDirection;
    private String meta;
    private String accentColor;
    private String icon;
}
