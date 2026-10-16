package com.finance.application.dto;

import com.finance.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountDto(
    @NotBlank(message = "Nome da conta é obrigatório")
    @Size(max = 100, message = "Nome da conta deve ter no máximo 100 caracteres")
    String name,

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    String description,

    @NotNull(message = "Tipo de conta é obrigatório")
    AccountType type,

    @NotNull(message = "Campo ativa é obrigatório")
    Boolean active
) {
}
