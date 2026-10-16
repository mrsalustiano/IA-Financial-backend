package com.finance.application.dto;

import jakarta.validation.constraints.Size;

public record CreateFinancialAnalysisDto(
    @Size(max = 500, message = "Foco da análise deve ter no máximo 500 caracteres")
    String focus
) {
}
