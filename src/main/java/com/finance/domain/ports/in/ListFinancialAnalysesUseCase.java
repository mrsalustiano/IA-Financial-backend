package com.finance.domain.ports.in;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialAnalysis;

public interface ListFinancialAnalysesUseCase {

    PageResult<FinancialAnalysis> handle(int page, int size);
}
