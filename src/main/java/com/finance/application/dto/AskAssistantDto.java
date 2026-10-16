package com.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskAssistantDto(
    @NotBlank(message = "Pergunta é obrigatória")
    @Size(max = 500, message = "Pergunta deve ter no máximo 500 caracteres")
    String question
) {
}
