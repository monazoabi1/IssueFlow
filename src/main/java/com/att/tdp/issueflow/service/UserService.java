package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    // -------------------------- GETTERS --------------------------
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    // Optional - “maybe there is a user” with that id / maybe not found
    // the caller can handle the null case
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Returns created user entity
    public UserEntity createUser(UserEntity user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserEntity saved = userRepository.save(user);
        auditLogService.log(AuditAction.CREATE, EntityType.USER, saved.getId());
        return saved;
    }

    // Returns updated user entity
    public UserEntity updateUser(Long id, UserEntity user) {
        
        UserEntity existingUser = getUserById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getFullName() != null) {
            existingUser.setFullName(user.getFullName());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        UserEntity saved = userRepository.save(existingUser);
        auditLogService.log(AuditAction.UPDATE, EntityType.USER, saved.getId());
        return saved;
    }

    // Deletes user entity
    public void deleteUser(Long id) {
        
        UserEntity existingUser = getUserById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long userId = existingUser.getId();
        userRepository.delete(existingUser);
        auditLogService.log(AuditAction.DELETE, EntityType.USER, userId);
    }
}