package com.finance.domain.ports.in;

import com.finance.application.command.CreateIncomeCommand;
import com.finance.domain.model.Income;

public interface CreateIncomeUseCase {

    Income handle(CreateIncomeCommand command);
}
