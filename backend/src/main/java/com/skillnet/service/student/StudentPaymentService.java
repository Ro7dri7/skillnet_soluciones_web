package com.skillnet.service.student;

import com.skillnet.mapper.PaymentMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.entity.payments.PaymentItem;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.web.dto.response.PaymentReceiptItemDTO;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import com.skillnet.web.dto.response.PaymentStatusResponseDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StudentPaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getMyPayments(Long userId) {
        return paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponseDTO getPaymentStatus(Long userId, Long paymentId) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        String courseTitle = payment.getCourse() != null ? payment.getCourse().getTitle() : null;
        Long courseId = payment.getCourse() != null ? payment.getCourse().getId() : null;

        return PaymentStatusResponseDTO.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .courseId(courseId)
                .courseTitle(courseTitle)
                .amount(payment.getAmount())
                .currency("USD")
                .createdAt(payment.getCreatedAt())
                .clientName(payment.getClientName())
                .clientEmail(payment.getClientEmail())
                .paymentMethod(payment.getPaymentMethod())
                .items(buildReceiptItems(payment))
                .build();
    }

    private List<PaymentReceiptItemDTO> buildReceiptItems(Payment payment) {
        List<PaymentReceiptItemDTO> items = new ArrayList<>();
        if (payment.getItems() != null && !payment.getItems().isEmpty()) {
            for (PaymentItem item : payment.getItems()) {
                Course course = item.getCourse();
                if (course == null) {
                    continue;
                }
                items.add(PaymentReceiptItemDTO.builder()
                        .courseId(course.getId())
                        .courseTitle(course.getTitle())
                        .courseFormat(course.getCourseFormat())
                        .build());
            }
            return items;
        }
        if (payment.getCourse() != null) {
            Course course = payment.getCourse();
            items.add(PaymentReceiptItemDTO.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseFormat(course.getCourseFormat())
                    .build());
        }
        return items;
    }
}
