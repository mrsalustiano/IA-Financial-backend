package com.finance.application.usecase;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialPlan;
import com.finance.domain.ports.in.ListFinancialPlansUseCase;
import com.finance.domain.ports.out.FinancialPlanRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListFinancialPlansUseCaseImpl implements ListFinancialPlansUseCase {

    private final FinancialPlanRepositoryPort planRepositoryPort;

    public ListFinancialPlansUseCaseImpl(FinancialPlanRepositoryPort planRepositoryPort) {
        this.planRepositoryPort = planRepositoryPort;
    }

    @Override
    public PageResult<FinancialPlan> handle(int page, int size) {
        return planRepositoryPort.search(page, size);
    }
}
