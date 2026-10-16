package com.finance.domain.ports.in;

import com.finance.application.command.UpdateIncomeCommand;
import com.finance.domain.model.Income;

public interface UpdateIncomeUseCase {

    Income handle(String id, UpdateIncomeCommand command);
}
