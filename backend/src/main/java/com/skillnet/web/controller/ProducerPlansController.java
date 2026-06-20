package com.skillnet.web.controller;

import com.skillnet.api.dto.payments.CheckoutPaymentResponseDTO;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.payments.ServiceOfferingPaymentService;
import com.skillnet.service.producer.ProducerPlansService;
import com.skillnet.web.dto.request.ServiceOfferingCheckoutRequestDTO;
import com.skillnet.web.dto.response.ProducerCapabilityStatusDTO;
import com.skillnet.web.dto.response.ServiceEntitlementResponseDTO;
import com.skillnet.web.dto.response.ServiceOfferingResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/producer/plans")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerPlansController {

    private final ProducerPlansService producerPlansService;
    private final ServiceOfferingPaymentService serviceOfferingPaymentService;

    @GetMapping("/offerings")
    public ResponseEntity<List<ServiceOfferingResponseDTO>> offerings() {
        return ResponseEntity.ok(producerPlansService.listActiveOfferings());
    }

    @GetMapping("/entitlements")
    public ResponseEntity<List<ServiceEntitlementResponseDTO>> entitlements(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerPlansService.listEntitlements(userDetails.getId()));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, ProducerCapabilityStatusDTO>> capabilities(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerPlansService.capabilities(userDetails.getId()));
    }

    @PostMapping("/checkout/stripe")
    public ResponseEntity<CheckoutPaymentResponseDTO> checkoutStripe(
            @Valid @RequestBody ServiceOfferingCheckoutRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(serviceOfferingPaymentService.purchasePlan(request, userDetails.getId()));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
