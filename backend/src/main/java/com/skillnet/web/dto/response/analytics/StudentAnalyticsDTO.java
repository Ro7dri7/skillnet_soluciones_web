package com.skillnet.web.dto.response.analytics;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentAnalyticsDTO {

    private StudentKpiDTO kpis = new StudentKpiDTO();
    private List<DailyCountDTO> purchaseTrend = new ArrayList<>();
    private List<CategoryProgressDTO> progressByCategory = new ArrayList<>();
    private List<StudentLearningCourseDTO> learningCourses = new ArrayList<>();
}
