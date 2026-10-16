package com.finance.domain.ports.in;

import com.finance.domain.model.FinancialAnalysis;

public interface GetFinancialAnalysisByIdUseCase {

    FinancialAnalysis handle(String id);
}
