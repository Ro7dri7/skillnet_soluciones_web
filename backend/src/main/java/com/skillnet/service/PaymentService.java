package com.skillnet.service;

import com.skillnet.web.dto.request.PaymentRequestDTO;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import java.util.List;
import java.util.Optional;

public interface PaymentService {

    PaymentResponseDTO create(PaymentRequestDTO dto);

    Optional<PaymentResponseDTO> update(Long id, PaymentRequestDTO dto);

    void deleteById(Long id);

    Optional<PaymentResponseDTO> findById(Long id);

    List<PaymentResponseDTO> findAll();

    List<PaymentResponseDTO> findByUserId(Long userId);

    List<PaymentResponseDTO> findByUserIdAndStatus(Long userId, String status);

    List<PaymentResponseDTO> findByStatus(String status);
}
