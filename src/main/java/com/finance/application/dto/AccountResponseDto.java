package com.finance.application.dto;

import com.finance.domain.enums.AccountType;

import java.time.Instant;

public record AccountResponseDto(
    String id,
    String userId,
    String name,
    String description,
    AccountType type,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
