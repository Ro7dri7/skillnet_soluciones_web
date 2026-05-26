package com.skillnet.web.controller;

import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CurriculumControllerDeleteLessonTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() {
        User user = userRepository.findById(1L).orElseThrow();
        CustomUserDetails details = new CustomUserDetails(user, "infoproductor");
        token = jwtService.generateToken(details, "infoproductor");
    }

    @Test
    void deleteLesson_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/lessons/2")
                        .header("Authorization", "Bearer " + token)
                        .header("Origin", "http://localhost:4200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
