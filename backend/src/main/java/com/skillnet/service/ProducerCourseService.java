package com.skillnet.service;

import com.skillnet.web.dto.request.CreateCourseCouponRequestDTO;
import com.skillnet.web.dto.request.CreateCourseRequestDTO;
import com.skillnet.web.dto.request.UpdateCourseBasicsRequestDTO;
import com.skillnet.web.dto.request.UpdateCourseMessagesRequestDTO;
import com.skillnet.web.dto.request.UpdateCoursePricingRequestDTO;
import com.skillnet.web.dto.response.CourseBasicsResponseDTO;
import com.skillnet.web.dto.response.CourseCouponResponseDTO;
import com.skillnet.web.dto.response.CourseMediaUploadResponseDTO;
import com.skillnet.web.dto.response.CourseMessagesResponseDTO;
import com.skillnet.web.dto.response.CoursePricingResponseDTO;
import com.skillnet.web.dto.response.ProducerCourseSummaryDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ProducerCourseService {

    ProducerCourseSummaryDTO createDraftCourse(Long professorId, CreateCourseRequestDTO dto);

    List<ProducerCourseSummaryDTO> getMyCourses(Long professorId);

    ProducerCourseSummaryDTO publishCourse(Long courseId, Long professorId);

    ProducerCourseSummaryDTO unpublishCourse(Long courseId, Long professorId);

    CourseBasicsResponseDTO updateBasics(Long courseId, Long professorId, UpdateCourseBasicsRequestDTO dto);

    CourseMediaUploadResponseDTO uploadCourseMedia(
            Long courseId,
            Long professorId,
            String kind,
            String originalFilename,
            String contentType,
            java.io.InputStream input,
            long size)
            throws java.io.IOException;

    CoursePricingResponseDTO updatePricing(
            Long courseId, Long professorId, UpdateCoursePricingRequestDTO dto);

    CourseMessagesResponseDTO updateMessages(
            Long courseId, Long professorId, UpdateCourseMessagesRequestDTO dto);

    List<CourseCouponResponseDTO> listCoupons(Long courseId, Long professorId);

    CourseCouponResponseDTO createCoupon(
            Long courseId, Long professorId, CreateCourseCouponRequestDTO dto);

    void deleteCoupon(Long courseId, Long professorId, Long couponId);
}
