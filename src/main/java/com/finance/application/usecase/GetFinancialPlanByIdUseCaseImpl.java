package com.finance.application.usecase;

import com.finance.domain.model.FinancialPlan;
import com.finance.domain.ports.in.GetFinancialPlanByIdUseCase;
import com.finance.domain.ports.out.FinancialPlanRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetFinancialPlanByIdUseCaseImpl implements GetFinancialPlanByIdUseCase {

    private final FinancialPlanRepositoryPort planRepositoryPort;

    public GetFinancialPlanByIdUseCaseImpl(FinancialPlanRepositoryPort planRepositoryPort) {
        this.planRepositoryPort = planRepositoryPort;
    }

    @Override
    public FinancialPlan handle(String id) {
        return planRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Plano financeiro não encontrado: " + id));
    }
}
