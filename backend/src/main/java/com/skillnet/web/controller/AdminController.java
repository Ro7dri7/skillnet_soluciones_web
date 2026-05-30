package com.skillnet.web.controller;

import com.skillnet.service.admin.AdminDashboardService;
import com.skillnet.service.admin.AdminCourseService;
import com.skillnet.web.dto.response.AdminDashboardResponseDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminCourseService adminCourseService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponseDTO> dashboard(
            @RequestParam(defaultValue = "semana") String period,
            @RequestParam(defaultValue = "resumen") String view,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(adminDashboardService.getDashboard(period, view, startDate, endDate));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDTO>> listCourses() {
        return ResponseEntity.ok(adminCourseService.listCourses());
    }

    @PutMapping("/courses/{courseId}/publish")
    public ResponseEntity<CourseResponseDTO> publishCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(adminCourseService.publishCourse(courseId));
    }

    @PutMapping("/courses/{courseId}/draft")
    public ResponseEntity<CourseResponseDTO> setDraft(@PathVariable Long courseId) {
        return ResponseEntity.ok(adminCourseService.setDraft(courseId));
    }

    @PutMapping("/courses/{courseId}/takedown")
    public ResponseEntity<CourseResponseDTO> takedownCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(adminCourseService.takedownCourse(courseId));
    }
}
