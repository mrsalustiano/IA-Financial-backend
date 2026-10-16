package com.finance.application.usecase;

import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.ListMonthlyClosingsUseCase;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMonthlyClosingsUseCaseImpl implements ListMonthlyClosingsUseCase {

    private final MonthlyClosingRepositoryPort closingRepositoryPort;

    public ListMonthlyClosingsUseCaseImpl(MonthlyClosingRepositoryPort closingRepositoryPort) {
        this.closingRepositoryPort = closingRepositoryPort;
    }

    @Override
    public List<MonthlyClosing> handle() {
        return closingRepositoryPort.findAll();
    }
}
