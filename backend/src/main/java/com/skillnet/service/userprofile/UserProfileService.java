package com.skillnet.service.userprofile;

import com.skillnet.domain.AuditAction;
import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.web.dto.request.ChangePasswordRequestDTO;
import com.skillnet.web.dto.request.UpdateUserProfileRequestDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserResponseDTO getMyProfile(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return userMapper.toResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO updateMyProfile(Long userId, UpdateUserProfileRequestDTO dto) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<String> changedFields = new ArrayList<>();

        if (dto.getFirstName() != null) {
            trackStringChange(changedFields, "firstName", user.getFirstName(), dto.getFirstName());
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            trackStringChange(changedFields, "lastName", user.getLastName(), dto.getLastName());
            user.setLastName(dto.getLastName());
        }
        if (dto.getBio() != null) {
            trackStringChange(changedFields, "bio", user.getBio(), dto.getBio());
            user.setBio(dto.getBio());
        }
        if (dto.getProfessionalTitle() != null) {
            trackStringChange(
                    changedFields, "professionalTitle", user.getProfessionalTitle(), dto.getProfessionalTitle());
            user.setProfessionalTitle(dto.getProfessionalTitle());
        }
        if (dto.getYearsExperience() != null) {
            if (!Objects.equals(user.getYearsExperience(), dto.getYearsExperience())) {
                changedFields.add("yearsExperience");
            }
            user.setYearsExperience(dto.getYearsExperience());
        }
        if (dto.getSpecialties() != null) {
            changedFields.add("specialties");
            user.setSpecialties(dto.getSpecialties());
        }
        if (dto.getSocialLinks() != null) {
            changedFields.add("socialLinks");
            user.setSocialLinks(dto.getSocialLinks());
        }
        if (dto.getProfileVisibility() != null) {
            if (!Objects.equals(user.isProfileVisibility(), dto.getProfileVisibility())) {
                changedFields.add("profileVisibility");
            }
            user.setProfileVisibility(dto.getProfileVisibility());
        }
        if (dto.getPhone() != null) {
            trackStringChange(changedFields, "phone", user.getPhone(), dto.getPhone());
            user.setPhone(dto.getPhone());
        }
        if (dto.getCountryCode() != null) {
            trackStringChange(changedFields, "countryCode", user.getCountryCode(), dto.getCountryCode());
            user.setCountryCode(dto.getCountryCode());
        }
        if (dto.getBirthDate() != null) {
            if (!Objects.equals(user.getBirthDate(), dto.getBirthDate())) {
                changedFields.add("birthDate");
            }
            user.setBirthDate(dto.getBirthDate());
        }
        if (dto.getLogo() != null) {
            trackStringChange(changedFields, "logo", user.getLogo(), dto.getLogo());
            user.setLogo(dto.getLogo());
        }
        if (dto.getProfilePicture() != null) {
            trackStringChange(changedFields, "profilePicture", user.getProfilePicture(), dto.getProfilePicture());
            user.setProfilePicture(dto.getProfilePicture());
        }
        if (dto.getCompany() != null) {
            trackStringChange(changedFields, "company", user.getCompany(), dto.getCompany());
            user.setCompany(dto.getCompany());
        }
        if (dto.getLocation() != null) {
            trackStringChange(changedFields, "location", user.getLocation(), dto.getLocation());
            user.setLocation(dto.getLocation());
        }
        if (dto.getWebsite() != null) {
            trackStringChange(changedFields, "website", user.getWebsite(), dto.getWebsite());
            user.setWebsite(dto.getWebsite());
        }
        if (dto.getLinkedinUrl() != null) {
            trackStringChange(changedFields, "linkedinUrl", user.getLinkedinUrl(), dto.getLinkedinUrl());
            user.setLinkedinUrl(dto.getLinkedinUrl());
        }
        if (dto.getTwitterUrl() != null) {
            trackStringChange(changedFields, "twitterUrl", user.getTwitterUrl(), dto.getTwitterUrl());
            user.setTwitterUrl(dto.getTwitterUrl());
        }
        if (dto.getYoutubeUrl() != null) {
            trackStringChange(changedFields, "youtubeUrl", user.getYoutubeUrl(), dto.getYoutubeUrl());
            user.setYoutubeUrl(dto.getYoutubeUrl());
        }
        if (dto.getInstagramUrl() != null) {
            trackStringChange(changedFields, "instagramUrl", user.getInstagramUrl(), dto.getInstagramUrl());
            user.setInstagramUrl(dto.getInstagramUrl());
        }
        if (dto.getPostalCode() != null) {
            trackStringChange(changedFields, "postalCode", user.getPostalCode(), dto.getPostalCode());
            user.setPostalCode(dto.getPostalCode());
        }
        if (dto.getAddress() != null) {
            trackStringChange(changedFields, "address", user.getAddress(), dto.getAddress());
            user.setAddress(dto.getAddress());
        }

        User saved = userRepository.save(user);
        if (!changedFields.isEmpty()) {
            auditService.logAction(
                    AuditAction.UPDATE_PROFILE,
                    AuditAction.ENTITY_USER,
                    saved.getId(),
                    saved.getEmail(),
                    "Perfil actualizado — campos: " + String.join(", ", changedFields));
        }
        return userMapper.toResponseDTO(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO dto) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        auditService.logAction(
                AuditAction.CHANGE_PASSWORD,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "Contraseña cambiada desde el perfil");
    }

    private void trackStringChange(List<String> changedFields, String field, String oldValue, String newValue) {
        if (!Objects.equals(normalize(oldValue), normalize(newValue))) {
            changedFields.add(field);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
