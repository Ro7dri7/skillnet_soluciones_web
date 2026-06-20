package com.skillnet.service.impl;

import com.skillnet.domain.AuditAction;
import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.UserService;
import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, AuditService auditService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        User user = userMapper.toEntity(dto);
        User saved = userRepository.save(user);
        auditService.logAction(
                AuditAction.REGISTER_USER,
                AuditAction.ENTITY_USER,
                saved.getId(),
                saved.getEmail(),
                "Registro de cuenta: " + saved.getEmail());
        return userMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public Optional<UserResponseDTO> update(Long id, UserRequestDTO dto) {
        return userRepository.findById(id).map(existing -> {
            userMapper.applyToEntity(existing, dto, false);
            User saved = userRepository.save(existing);
            auditService.logAction(
                    AuditAction.UPDATE_PROFILE,
                    AuditAction.ENTITY_USER,
                    saved.getId(),
                    saved.getEmail(),
                    "Usuario actualizado (admin/API): " + saved.getEmail());
            return userMapper.toResponseDTO(saved);
        });
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository
                .findById(id)
                .ifPresent(user -> auditService.logAction(
                        AuditAction.DELETE_USER,
                        AuditAction.ENTITY_USER,
                        user.getId(),
                        user.getEmail(),
                        "Cuenta eliminada: " + user.getEmail()));
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> findById(Long id) {
        return userRepository.findById(id).map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toResponseDTO);
    }
}
