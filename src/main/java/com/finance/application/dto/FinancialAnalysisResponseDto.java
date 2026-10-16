package com.finance.application.dto;

import java.time.Instant;
import java.util.List;

public record FinancialAnalysisResponseDto(
    String id,
    String userId,
    String prompt,
    String response,
    long score,
    List<String> recommendations,
    String provider,
    String model,
    Instant createdAt
) {
}
