package com.finance.application.command;

public record UpdateUserCommand(String email, String cpf) {

    public UpdateUserCommand {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (cpf != null) {
            cpf = cpf.trim();
        }
    }
}
