package com.finance.application.usecase;

import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.GetMonthlyClosingByIdUseCase;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetMonthlyClosingByIdUseCaseImpl implements GetMonthlyClosingByIdUseCase {

    private final MonthlyClosingRepositoryPort closingRepositoryPort;
    private final CurrentUserPort currentUserPort;

    public GetMonthlyClosingByIdUseCaseImpl(MonthlyClosingRepositoryPort closingRepositoryPort,
                                            CurrentUserPort currentUserPort) {
        this.closingRepositoryPort = closingRepositoryPort;
        this.currentUserPort = currentUserPort;
    }

    @Override
    public MonthlyClosing handle(String id) {
        return closingRepositoryPort.findByIdAndUser(currentUserPort.requireUserId(), id)
            .orElseThrow(() -> new NotFoundException("Fechamento mensal não encontrado: " + id));
    }
}
