package com.finance.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserDto(
    @NotBlank @Email(message = "deve ser um e-mail válido") String email,
    @NotBlank String cpf) {
}
