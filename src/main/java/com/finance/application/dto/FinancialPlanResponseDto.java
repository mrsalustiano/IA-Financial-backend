package com.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinancialPlanResponseDto(String id, String userId, String goal, BigDecimal targetAmount,
                                       LocalDate targetDate, String prompt, String response,
                                       List<String> recommendations, String provider, String model,
                                       Instant createdAt) {
}
