package com.finance.application.dto;

import java.time.Instant;

public record AssistantMessageResponseDto(String id, String userId, String question, String answer,
                                          String provider, String model, Instant createdAt) {
}
