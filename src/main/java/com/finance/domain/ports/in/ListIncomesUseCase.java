package com.finance.domain.ports.in;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;

public interface ListIncomesUseCase {

    PageResult<Income> handle(IncomeSearchQuery query);
}
