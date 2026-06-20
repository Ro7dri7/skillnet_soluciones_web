package com.skillnet.web.controller;

import com.skillnet.service.publicprofile.PublicProfileService;
import com.skillnet.web.dto.response.PublicInfoproductorProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @GetMapping("/infoproductors/{username}")
    public ResponseEntity<PublicInfoproductorProfileResponseDTO> getInfoproductorProfile(
            @PathVariable String username) {
        return ResponseEntity.ok(publicProfileService.getInfoproductorProfile(username));
    }
}
