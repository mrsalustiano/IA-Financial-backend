package com.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyClosingResponseDto(String id, String userId, String referenceMonth, BigDecimal totalIncome,
                                        BigDecimal totalExpense, BigDecimal balance, Instant closedAt,
                                        Instant createdAt) {
}
