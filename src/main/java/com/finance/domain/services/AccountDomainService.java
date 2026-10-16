package com.finance.domain.services;

import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.BusinessException;
import com.finance.shared.exception.NotFoundException;

public class AccountDomainService {

    private final AccountRepositoryPort accountRepositoryPort;

    public AccountDomainService(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    public Account requireActiveAccount(String accountId) {
        Account account = accountRepositoryPort.findById(accountId)
            .orElseThrow(() -> new NotFoundException("Conta não encontrada: " + accountId));
        if (!account.isActive()) {
            throw new BusinessException("Conta inativa: " + account.getName());
        }
        return account;
    }
}
