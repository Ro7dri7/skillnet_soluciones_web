package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProducerCourseSummaryDTO {

    private Long id;
    private String title;
    private String courseFormat;
    private String status;
    private Instant createdAt;
    private String imageUrl;
}
