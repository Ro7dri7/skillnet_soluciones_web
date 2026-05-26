package com.skillnet.mapper;

import com.skillnet.persistence.entity.core.User;
import com.skillnet.service.UserRoleNormalizer;
import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.ProfessorSummaryDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import com.skillnet.web.dto.response.UserSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setLastLogin(user.getLastLogin());
        dto.setSuperUser(user.isSuperUser());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setStaff(user.isStaff());
        dto.setActive(user.isActive());
        dto.setDateJoined(user.getDateJoined());
        dto.setRole(user.getRole());
        dto.setStudent(user.isStudent());
        dto.setInfoproductor(user.isInfoproductor());
        dto.setAffiliate(user.isAffiliate());
        dto.setActiveRole(user.getActiveRole());
        dto.setPhone(user.getPhone());
        dto.setCountryCode(user.getCountryCode());
        dto.setPhoneVerified(user.isPhoneVerified());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setBirthDate(user.getBirthDate());
        dto.setLogo(user.getLogo());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setBio(user.getBio());
        dto.setProfessionalTitle(user.getProfessionalTitle());
        dto.setYearsExperience(user.getYearsExperience());
        dto.setSpecialties(user.getSpecialties());
        dto.setSocialLinks(user.getSocialLinks());
        dto.setProfileVisibility(user.isProfileVisibility());
        dto.setCompany(user.getCompany());
        dto.setLocation(user.getLocation());
        dto.setWebsite(user.getWebsite());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setTwitterUrl(user.getTwitterUrl());
        dto.setYoutubeUrl(user.getYoutubeUrl());
        dto.setInstagramUrl(user.getInstagramUrl());
        dto.setDocumentNumber(user.getDocumentNumber());
        dto.setIdentityVerified(user.isIdentityVerified());
        dto.setIdentityVerifiedAt(user.getIdentityVerifiedAt());
        dto.setVerifiedInfoproductor(user.isVerifiedInfoproductor());
        dto.setVerifiedUntil(user.getVerifiedUntil());
        dto.setIdentityProvider(user.getIdentityProvider());
        dto.setIdentityReference(user.getIdentityReference());
        dto.setBehaviorVerified(user.isBehaviorVerified());
        dto.setBehaviorVerifiedAt(user.getBehaviorVerifiedAt());
        dto.setPostalCode(user.getPostalCode());
        dto.setAddress(user.getAddress());
        return dto;
    }

    public UserSummaryDTO toSummaryDTO(User user) {
        if (user == null) {
            return null;
        }
        return toSummaryDTO(user, com.skillnet.security.RoleAuthorityResolver.defaultActiveRole(user));
    }

    public UserSummaryDTO toSummaryDTO(User user, String activeRole) {
        if (user == null) {
            return null;
        }
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(activeRole);
        dto.setActiveRole(activeRole);
        dto.setStudent(user.isStudent());
        dto.setInfoproductor(user.isInfoproductor());
        dto.setProfilePicture(user.getProfilePicture());
        return dto;
    }

    public ProfessorSummaryDTO toProfessorSummaryDTO(User user) {
        if (user == null) {
            return null;
        }
        return new ProfessorSummaryDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName());
    }

    public User toEntity(UserRequestDTO dto) {
        User user = new User();
        applyToEntity(user, dto, true);
        UserRoleNormalizer.applyDualRoleCapabilities(user, dto.getRole());
        return user;
    }

    public void applyToEntity(User user, UserRequestDTO dto, boolean isCreate) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(dto.getPassword());
        } else if (isCreate) {
            user.setPassword("");
        }
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setStaff(dto.isStaff());
        user.setActive(dto.isActive());
        if (dto.getDateJoined() != null) {
            user.setDateJoined(dto.getDateJoined());
        }
        user.setRole(dto.getRole());
        user.setStudent(dto.isStudent());
        user.setInfoproductor(dto.isInfoproductor());
        user.setAffiliate(dto.isAffiliate());
        user.setActiveRole(dto.getActiveRole());
        user.setPhone(dto.getPhone());
        user.setCountryCode(dto.getCountryCode());
        user.setPhoneVerified(dto.isPhoneVerified());
        user.setEmailVerified(dto.isEmailVerified());
        user.setBirthDate(dto.getBirthDate());
        user.setLogo(dto.getLogo());
        user.setProfilePicture(dto.getProfilePicture());
        user.setBio(dto.getBio());
        user.setProfessionalTitle(dto.getProfessionalTitle());
        user.setYearsExperience(dto.getYearsExperience());
        user.setSpecialties(dto.getSpecialties());
        user.setSocialLinks(dto.getSocialLinks());
        user.setProfileVisibility(dto.isProfileVisibility());
        user.setCompany(dto.getCompany());
        user.setLocation(dto.getLocation());
        user.setWebsite(dto.getWebsite());
        user.setLinkedinUrl(dto.getLinkedinUrl());
        user.setTwitterUrl(dto.getTwitterUrl());
        user.setYoutubeUrl(dto.getYoutubeUrl());
        user.setInstagramUrl(dto.getInstagramUrl());
        user.setDocumentNumber(dto.getDocumentNumber());
        user.setIdentityVerified(dto.isIdentityVerified());
        user.setIdentityVerifiedAt(dto.getIdentityVerifiedAt());
        user.setVerifiedInfoproductor(dto.isVerifiedInfoproductor());
        user.setVerifiedUntil(dto.getVerifiedUntil());
        user.setIdentityProvider(dto.getIdentityProvider());
        user.setIdentityReference(dto.getIdentityReference());
        user.setBehaviorVerified(dto.isBehaviorVerified());
        user.setBehaviorVerifiedAt(dto.getBehaviorVerifiedAt());
        user.setPostalCode(dto.getPostalCode());
        user.setAddress(dto.getAddress());
    }
}
