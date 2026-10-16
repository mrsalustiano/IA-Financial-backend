package com.finance.application.dto;

import com.finance.domain.enums.CategoryClassification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryDto(
    @NotBlank(message = "Nome da categoria é obrigatório")
    @Size(max = 100, message = "Nome da categoria deve ter no máximo 100 caracteres")
    String name,

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    String description,

    @NotNull(message = "Classificação da categoria é obrigatória")
    CategoryClassification classification,

    @NotNull(message = "Campo ativa é obrigatório")
    Boolean active
) {
}
