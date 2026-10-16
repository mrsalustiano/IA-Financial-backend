package com.finance.application.dto;

import java.time.Instant;

public record UserResponseDto(String id, String email, String cpf, boolean active, Instant createdAt) {
}
