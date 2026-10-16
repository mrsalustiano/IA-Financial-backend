package com.finance.application.usecase;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.ListIncomesUseCase;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListIncomesUseCaseImpl implements ListIncomesUseCase {

    private final IncomeRepositoryPort incomeRepositoryPort;

    public ListIncomesUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
    }

    @Override
    public PageResult<Income> handle(IncomeSearchQuery query) {
        return incomeRepositoryPort.search(query);
    }
}
