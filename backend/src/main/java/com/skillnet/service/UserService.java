package com.skillnet.service;

import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import java.util.List;
import java.util.Optional;

public interface UserService {

    UserResponseDTO create(UserRequestDTO dto);

    Optional<UserResponseDTO> update(Long id, UserRequestDTO dto);

    void deleteById(Long id);

    Optional<UserResponseDTO> findById(Long id);

    List<UserResponseDTO> findAll();

    Optional<UserResponseDTO> findByEmail(String email);

    Optional<UserResponseDTO> findByUsername(String username);
}
