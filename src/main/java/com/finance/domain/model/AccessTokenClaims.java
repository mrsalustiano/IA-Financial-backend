package com.finance.domain.model;

import java.time.Instant;

public record AccessTokenClaims(String userId, String email, Instant issuedAt, Instant expiresAt) {
}
