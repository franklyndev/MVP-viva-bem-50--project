package br.com.vivabem50.config;

import org.springframework.security.oauth2.jwt.Jwt;

/** O "subject" do token é o id do usuário. */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
