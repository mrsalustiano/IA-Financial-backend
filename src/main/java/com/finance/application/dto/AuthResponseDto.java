package com.finance.application.dto;

import java.time.Instant;

public record AuthResponseDto(String accessToken, String refreshToken, String tokenType, long expiresInSeconds,
                              AuthUserDto user) {

    public record AuthUserDto(String id, String email, boolean active, Instant createdAt) {
    }
}
