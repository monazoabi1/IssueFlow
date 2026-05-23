package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.ForbiddenException;
import com.att.tdp.issueflow.Exception.UnauthorizedException;
import com.att.tdp.issueflow.dto.LoginRequest;
import com.att.tdp.issueflow.dto.LoginResponse;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = authenticate(request.getUsername(), request.getPassword());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    public void logout(String bearerToken) {
        String token = extractBearerToken(bearerToken);
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        tokenBlacklistService.revoke(token, jwtService.extractExpiration(token));
    }

    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public void requireAdmin() {
        UserEntity user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
    }

    public UserEntity authenticate(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return user;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
