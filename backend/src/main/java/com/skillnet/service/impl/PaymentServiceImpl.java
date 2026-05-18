package com.skillnet.service.impl;

import com.skillnet.mapper.PaymentMapper;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.service.PaymentService;
import com.skillnet.web.dto.request.PaymentRequestDTO;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentResponseDTO create(PaymentRequestDTO dto) {
        Payment payment = paymentMapper.toEntity(dto);
        return paymentMapper.toResponseDTO(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public Optional<PaymentResponseDTO> update(Long id, PaymentRequestDTO dto) {
        return paymentRepository.findById(id).map(existing -> {
            paymentMapper.applyToEntity(existing, dto, false);
            return paymentMapper.toResponseDTO(paymentRepository.save(existing));
        });
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        paymentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentResponseDTO> findById(Long id) {
        return paymentRepository.findById(id).map(paymentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> findAll() {
        return paymentRepository.findAll().stream().map(paymentMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> findByUserId(Long userId) {
        return paymentRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> findByUserIdAndStatus(Long userId, String status) {
        return paymentRepository.findByUser_IdAndStatus(userId, status).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> findByStatus(String status) {
        return paymentRepository.findByStatus(status).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
}
