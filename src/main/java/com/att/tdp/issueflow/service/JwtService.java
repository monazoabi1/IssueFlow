package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.config.JwtProperties;
import com.att.tdp.issueflow.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(JwtProperties jwtProperties, TokenBlacklistService tokenBlacklistService) {
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank() || tokenBlacklistService.isRevoked(token)) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.toInstant();
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
