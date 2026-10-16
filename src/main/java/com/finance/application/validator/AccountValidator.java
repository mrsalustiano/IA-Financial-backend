package com.finance.application.validator;

import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

@Component
public class AccountValidator {

    private final AccountRepositoryPort accountRepositoryPort;

    public AccountValidator(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    public void ensureNameAvailable(String name, String excludeId) {
        if (accountRepositoryPort.existsByName(name, excludeId)) {
            throw new ConflictException("Já existe uma conta com o nome: " + name);
        }
    }
}
