package com.finance.application.dto;

import com.finance.domain.enums.CategoryClassification;

import java.time.Instant;

public record CategoryResponseDto(
    String id,
    String userId,
    String name,
    String description,
    CategoryClassification classification,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
