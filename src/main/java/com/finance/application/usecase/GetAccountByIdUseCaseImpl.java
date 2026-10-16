package com.finance.application.usecase;

import com.finance.domain.model.Account;
import com.finance.domain.ports.in.GetAccountByIdUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetAccountByIdUseCaseImpl implements GetAccountByIdUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public GetAccountByIdUseCaseImpl(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
    public Account handle(String id) {
        return accountRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Conta não encontrada: " + id));
    }
}
