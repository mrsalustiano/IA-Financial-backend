package com.finance.domain.ports.in;

import com.finance.application.command.GenerateFinancialAnalysisCommand;
import com.finance.domain.model.FinancialAnalysis;

public interface GenerateFinancialAnalysisUseCase {

    FinancialAnalysis handle(GenerateFinancialAnalysisCommand command);
}
