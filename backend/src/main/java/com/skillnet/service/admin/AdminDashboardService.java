package com.skillnet.service.admin;

import com.skillnet.web.dto.response.AdminDashboardResponseDTO;
import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDashboardResponseDTO getDashboard(
            String period, String view, LocalDate customStart, LocalDate customEnd);
}
