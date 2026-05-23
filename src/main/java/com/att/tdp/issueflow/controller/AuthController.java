package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.LoginRequest;
import com.att.tdp.issueflow.dto.LoginResponse;
import com.att.tdp.issueflow.dto.UserResponse;
import com.att.tdp.issueflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return new UserResponse(authService.getCurrentUser());
    }
}
