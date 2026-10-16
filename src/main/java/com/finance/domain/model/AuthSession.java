package com.finance.domain.model;

import java.time.Instant;

/**
 * Sessao de autenticacao: par de tokens emitidos no login/refresh junto do usuario autenticado.
 */
public record AuthSession(User user, String accessToken, String refreshToken, String tokenType,
                          long expiresInSeconds) {

    public static AuthSession of(User user, String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthSession(user, accessToken, refreshToken, "Bearer", expiresInSeconds);
    }

    public Instant issuedAt() {
        return Instant.now();
    }
}
