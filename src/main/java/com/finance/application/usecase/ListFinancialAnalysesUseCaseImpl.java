package com.finance.application.usecase;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.ports.in.ListFinancialAnalysesUseCase;
import com.finance.domain.ports.out.FinancialAnalysisRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListFinancialAnalysesUseCaseImpl implements ListFinancialAnalysesUseCase {

    private final FinancialAnalysisRepositoryPort analysisRepositoryPort;

    public ListFinancialAnalysesUseCaseImpl(FinancialAnalysisRepositoryPort analysisRepositoryPort) {
        this.analysisRepositoryPort = analysisRepositoryPort;
    }

    @Override
    public PageResult<FinancialAnalysis> handle(int page, int size) {
        return analysisRepositoryPort.search(page, size);
    }
}
