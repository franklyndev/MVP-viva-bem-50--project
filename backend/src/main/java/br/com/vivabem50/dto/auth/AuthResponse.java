package br.com.vivabem50.dto.auth;

import br.com.vivabem50.dto.profile.UserResponse;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
}
