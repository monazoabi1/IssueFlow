package com.att.tdp.issueflow.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiresAt) {
        if (token != null && !token.isBlank()) {
            revokedTokens.put(token, expiresAt);
        }
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 300_000)
    void purgeExpired() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
