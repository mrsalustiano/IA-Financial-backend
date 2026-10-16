package com.finance.domain.ports.in;

import com.finance.application.command.UpdateAccountCommand;
import com.finance.domain.model.Account;

public interface UpdateAccountUseCase {

    Account handle(String id, UpdateAccountCommand command);
}
