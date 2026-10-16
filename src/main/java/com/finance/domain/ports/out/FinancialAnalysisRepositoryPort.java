package com.finance.domain.ports.out;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialAnalysis;

import java.util.Optional;

public interface FinancialAnalysisRepositoryPort {

    FinancialAnalysis save(FinancialAnalysis analysis);

    Optional<FinancialAnalysis> findById(String id);

    PageResult<FinancialAnalysis> search(int page, int size);
}
