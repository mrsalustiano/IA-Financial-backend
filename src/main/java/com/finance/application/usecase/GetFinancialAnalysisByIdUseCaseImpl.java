package com.finance.application.usecase;

import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.ports.in.GetFinancialAnalysisByIdUseCase;
import com.finance.domain.ports.out.FinancialAnalysisRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetFinancialAnalysisByIdUseCaseImpl implements GetFinancialAnalysisByIdUseCase {

    private final FinancialAnalysisRepositoryPort analysisRepositoryPort;

    public GetFinancialAnalysisByIdUseCaseImpl(FinancialAnalysisRepositoryPort analysisRepositoryPort) {
        this.analysisRepositoryPort = analysisRepositoryPort;
    }

    @Override
    public FinancialAnalysis handle(String id) {
        return analysisRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Análise não encontrada: " + id));
    }
}
