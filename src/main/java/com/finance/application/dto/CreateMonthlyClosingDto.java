package com.finance.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMonthlyClosingDto(
    @NotBlank(message = "Mês é obrigatório")
    String month
) {
}
