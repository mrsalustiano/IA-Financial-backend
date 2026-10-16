package com.finance.adapters.out.security;

import com.finance.config.JwtProperties;
import com.finance.domain.model.AccessTokenClaims;
import com.finance.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderAdapterTest {

    private final JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(properties());

    private static JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("segredo-de-teste-com-mais-de-32-caracteres-para-hs256");
        properties.setIssuer("ia-financial-test");
        properties.setAccessTokenTtl(Duration.ofMinutes(5));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        return properties;
    }

    @Test
    void createsAndParsesAccessToken() {
        String token = adapter.createAccessToken("u-1", "a@test.com");

        AccessTokenClaims claims = adapter.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo("u-1");
        assertThat(claims.email()).isEqualTo("a@test.com");
        assertThat(claims.expiresAt()).isAfter(Instant.now());
        assertThat(adapter.accessTokenTtlSeconds()).isEqualTo(300L);
    }

    @Test
    void rejectsTamperedToken() {
        String token = adapter.createAccessToken("u-1", "a@test.com");
        assertThatThrownBy(() -> adapter.parseAccessToken(token.substring(0, token.length() - 2) + "xx"))
            .isInstanceOf(UnauthorizedException.class).hasMessageContaining("inválido");
        assertThatThrownBy(() -> adapter.parseAccessToken("lixo"))
            .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> adapter.parseAccessToken(null))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties expiredProperties = properties();
        expiredProperties.setAccessTokenTtl(Duration.ofSeconds(-10));
        JwtTokenProviderAdapter expiredAdapter = new JwtTokenProviderAdapter(expiredProperties);
        String token = expiredAdapter.createAccessToken("u-1", "a@test.com");

        assertThatThrownBy(() -> expiredAdapter.parseAccessToken(token))
            .isInstanceOf(UnauthorizedException.class).hasMessageContaining("expirado");
    }

    @Test
    void rejectsTokenFromDifferentIssuer() {
        JwtProperties otherIssuer = properties();
        otherIssuer.setIssuer("outro-issuer");
        JwtTokenProviderAdapter otherAdapter = new JwtTokenProviderAdapter(otherIssuer);
        String token = otherAdapter.createAccessToken("u-1", "a@test.com");

        assertThatThrownBy(() -> adapter.parseAccessToken(token))
            .isInstanceOf(UnauthorizedException.class);
    }
}
