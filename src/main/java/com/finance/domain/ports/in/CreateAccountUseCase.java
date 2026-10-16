package com.finance.domain.ports.in;

import com.finance.application.command.CreateAccountCommand;
import com.finance.domain.model.Account;

public interface CreateAccountUseCase {

    Account handle(CreateAccountCommand command);
}
