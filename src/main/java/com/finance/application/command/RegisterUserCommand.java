package com.finance.application.command;

public record RegisterUserCommand(String email, String cpf, String password, Boolean active) {

    public RegisterUserCommand {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (cpf != null) {
            cpf = cpf.trim();
        }
    }

    public boolean requestedActive() {
        return active == null || active;
    }
}
