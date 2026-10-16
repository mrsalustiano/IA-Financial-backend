package com.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateIncomeDto(
    @NotBlank(message = "Id da conta é obrigatório")
    String accountId,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    String description,

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero")
    BigDecimal amount,

    @NotNull(message = "Data de recebimento é obrigatória")
    @PastOrPresent(message = "Data de recebimento não pode ser futura")
    LocalDate receivedAt
) {
}
