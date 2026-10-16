package com.finance.application.usecase;

import com.finance.domain.model.Income;
import com.finance.domain.ports.in.GetIncomeByIdUseCase;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetIncomeByIdUseCaseImpl implements GetIncomeByIdUseCase {

    private final IncomeRepositoryPort incomeRepositoryPort;

    public GetIncomeByIdUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
    }

    @Override
    public Income handle(String id) {
        return incomeRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Receita não encontrada: " + id));
    }
}
