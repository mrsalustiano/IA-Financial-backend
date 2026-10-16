package com.finance.domain.ports.in;

import com.finance.domain.model.Account;

public interface GetAccountByIdUseCase {

    Account handle(String id);
}
