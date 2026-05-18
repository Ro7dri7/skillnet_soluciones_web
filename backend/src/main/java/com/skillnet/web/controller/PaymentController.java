package com.skillnet.web.controller;

import com.skillnet.service.PaymentService;
import com.skillnet.web.dto.request.PaymentRequestDTO;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> findAll() {
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + id)));
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO dto) {
        PaymentResponseDTO created = paymentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.ok(paymentService.update(id, dto)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (paymentService.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Payment not found with id: " + id);
        }
        paymentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
