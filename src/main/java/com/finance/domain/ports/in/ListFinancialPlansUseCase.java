package com.finance.domain.ports.in;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialPlan;

public interface ListFinancialPlansUseCase {

    PageResult<FinancialPlan> handle(int page, int size);
}
