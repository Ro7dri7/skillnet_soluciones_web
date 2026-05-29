package com.skillnet.api.controller.payments;

import com.skillnet.api.dto.payments.CheckoutRequestDTO;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.payments.StripePaymentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/checkout")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CheckoutController {

    private final StripePaymentService stripePaymentService;

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> processStripePayment(
            @RequestBody CheckoutRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        String responseMessage = stripePaymentService.processRealPayment(request, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", responseMessage, "status", "success"));
    }
}
