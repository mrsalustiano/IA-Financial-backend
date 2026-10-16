package com.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeResponseDto(
    String id,
    String userId,
    String accountId,
    String description,
    BigDecimal amount,
    LocalDate receivedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
