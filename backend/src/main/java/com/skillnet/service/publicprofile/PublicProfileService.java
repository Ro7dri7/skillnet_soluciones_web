package com.skillnet.service.publicprofile;

import com.skillnet.domain.CourseStatus;
import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.web.dto.response.CourseSummaryDTO;
import com.skillnet.web.dto.response.PublicInfoproductorProfileResponseDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Transactional(readOnly = true)
    public PublicInfoproductorProfileResponseDTO getInfoproductorProfile(String username) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!user.isInfoproductor() || !user.isProfileVisibility()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no disponible");
        }

        List<CourseSummaryDTO> publishedCourses = courseRepository.findByProfessor_Id(user.getId()).stream()
                .filter(course -> CourseStatus.PUBLISHED.getDbValue().equals(course.getStatus()))
                .map(courseMapper::toSummaryDTO)
                .toList();

        return PublicInfoproductorProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .professionalTitle(user.getProfessionalTitle())
                .yearsExperience(user.getYearsExperience())
                .specialties(user.getSpecialties())
                .socialLinks(user.getSocialLinks())
                .company(user.getCompany())
                .location(user.getLocation())
                .website(user.getWebsite())
                .linkedinUrl(user.getLinkedinUrl())
                .twitterUrl(user.getTwitterUrl())
                .youtubeUrl(user.getYoutubeUrl())
                .instagramUrl(user.getInstagramUrl())
                .profilePicture(user.getProfilePicture())
                .verifiedInfoproductor(user.isVerifiedInfoproductor())
                .publishedCourses(publishedCourses)
                .build();
    }
}
