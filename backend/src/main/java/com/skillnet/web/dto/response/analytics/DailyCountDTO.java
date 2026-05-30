package com.skillnet.web.dto.response.analytics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyCountDTO {

    private String date;
    private Long count;
}
