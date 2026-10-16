package com.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private final Instant now = Instant.parse("2026-10-16T12:00:00Z");
    private final Instant expires = now.plusSeconds(3600);

    @Test
    void createdTokenIsValidUntilExpiry() {
        RefreshToken token = RefreshToken.create("rt-1", "u-1", "hash", expires, now);

        assertThat(token.isValid(now)).isTrue();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired(now)).isFalse();
        assertThat(token.isExpired(expires)).isTrue();
    }

    @Test
    void revokedTokenIsNotValid() {
        RefreshToken token = RefreshToken.create("rt-1", "u-1", "hash", expires, now);

        token.revoke(now.plusSeconds(10));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isValid(now.plusSeconds(11))).isFalse();
        assertThat(token.getRevokedAt()).isEqualTo(now.plusSeconds(10));
        assertThat(token.getUpdatedAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    void rehydratePreservesState() {
        RefreshToken token = RefreshToken.rehydrate("rt-2", "u-1", "hash", expires, true, now, false, null, now, now);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isValid(now)).isFalse();
    }
}
