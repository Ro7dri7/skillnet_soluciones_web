package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.notification.NotificationService;
import com.skillnet.web.dto.response.NotificationCountResponseDTO;
import com.skillnet.web.dto.response.NotificationResponseDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(notificationService.listForUser(userDetails.getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponseDTO> countUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(notificationService.countUnread(userDetails.getId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long id) {
        requireAuth(userDetails);
        return ResponseEntity.ok(notificationService.markRead(userDetails.getId(), id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        int updated = notificationService.markAllRead(userDetails.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
