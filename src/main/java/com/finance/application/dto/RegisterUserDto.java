package com.finance.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserDto(
    @NotBlank @Email(message = "deve ser um e-mail válido") String email,
    @NotBlank String cpf,
    @NotBlank @Size(min = 8, message = "a senha deve ter no mínimo 8 caracteres") String password,
    Boolean active) {
}
