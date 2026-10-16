package com.finance.domain.model;

import java.time.Instant;

public class RefreshToken extends BaseEntity {

    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;

    private RefreshToken(String id, String userId, String tokenHash, Instant expiresAt, boolean revoked,
                         Instant revokedAt, boolean deleted, Instant deletedAt, Instant createdAt,
                         Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken create(String id, String userId, String tokenHash, Instant expiresAt, Instant now) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, false, null, false, null, now, now);
    }

    public static RefreshToken rehydrate(String id, String userId, String tokenHash, Instant expiresAt,
                                         boolean revoked, Instant revokedAt, boolean deleted, Instant deletedAt,
                                         Instant createdAt, Instant updatedAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revoked, revokedAt, deleted, deletedAt, createdAt,
            updatedAt);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isValid(Instant now) {
        return !revoked && !isExpired(now);
    }

    public void revoke(Instant now) {
        this.revoked = true;
        this.revokedAt = now;
        this.updatedAt = now;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
