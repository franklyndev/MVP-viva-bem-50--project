package br.com.vivabem50.dto.auth;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt, UserSummary user) {

    public record UserSummary(Long id, String name, String email) {
    }
}
