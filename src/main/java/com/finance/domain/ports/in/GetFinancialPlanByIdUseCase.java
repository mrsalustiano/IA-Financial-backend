package com.finance.domain.ports.in;

import com.finance.domain.model.FinancialPlan;

public interface GetFinancialPlanByIdUseCase {

    FinancialPlan handle(String id);
}
