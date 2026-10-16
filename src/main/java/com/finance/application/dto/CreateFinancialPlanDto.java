package com.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFinancialPlanDto(
    @NotBlank(message = "Meta financeira é obrigatória")
    @Size(max = 500, message = "Meta financeira deve ter no máximo 500 caracteres")
    String goal,

    @Positive(message = "Valor da meta deve ser maior que zero")
    BigDecimal targetAmount,

    LocalDate targetDate
) {
}
