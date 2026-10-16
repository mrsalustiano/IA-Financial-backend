package com.finance.application.usecase;

import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.ListMonthlyClosingsUseCase;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMonthlyClosingsUseCaseImpl implements ListMonthlyClosingsUseCase {

    private final MonthlyClosingRepositoryPort closingRepositoryPort;
    private final CurrentUserPort currentUserPort;

    public ListMonthlyClosingsUseCaseImpl(MonthlyClosingRepositoryPort closingRepositoryPort,
                                          CurrentUserPort currentUserPort) {
        this.closingRepositoryPort = closingRepositoryPort;
        this.currentUserPort = currentUserPort;
    }

    @Override
    public List<MonthlyClosing> handle() {
        return closingRepositoryPort.findAllByUser(currentUserPort.requireUserId());
    }
}
