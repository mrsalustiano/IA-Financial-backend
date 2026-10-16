package com.finance.domain.ports.in;

import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Account;

public interface ListAccountsUseCase {

    PageResult<Account> handle(AccountSearchQuery query);
}
