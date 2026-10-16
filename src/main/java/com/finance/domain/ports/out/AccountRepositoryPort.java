package com.finance.domain.ports.out;

import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Account;

import java.util.Optional;

public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(String id);

    boolean existsByName(String name, String excludeId);

    PageResult<Account> search(AccountSearchQuery query);
}
