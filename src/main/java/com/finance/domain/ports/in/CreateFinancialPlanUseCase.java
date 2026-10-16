package com.finance.domain.ports.in;

import com.finance.application.command.CreateFinancialPlanCommand;
import com.finance.domain.model.FinancialPlan;

public interface CreateFinancialPlanUseCase {

    FinancialPlan handle(CreateFinancialPlanCommand command);
}
