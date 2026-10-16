package com.finance.application.usecase;

import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Account;
import com.finance.domain.ports.in.ListAccountsUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListAccountsUseCaseImpl implements ListAccountsUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public ListAccountsUseCaseImpl(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
    public PageResult<Account> handle(AccountSearchQuery query) {
        return accountRepositoryPort.search(query);
    }
}
