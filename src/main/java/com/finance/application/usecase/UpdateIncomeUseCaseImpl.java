package com.finance.application.usecase;

import com.finance.application.command.UpdateIncomeCommand;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.UpdateIncomeUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.AccountDomainService;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateIncomeUseCaseImpl implements UpdateIncomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateIncomeUseCaseImpl.class);

    private final IncomeRepositoryPort incomeRepositoryPort;
    private final AccountDomainService accountDomainService;
    private final AuditPort auditPort;

    public UpdateIncomeUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort,
                                   AccountDomainService accountDomainService, AuditPort auditPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.accountDomainService = accountDomainService;
        this.auditPort = auditPort;
    }

    @Override
    public Income handle(String id, UpdateIncomeCommand command) {
        log.info("Updating income id={}", id);
        Instant now = Instant.now();
        Income income = incomeRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Receita não encontrada: " + id));
        Object oldValue = auditPort.snapshot(income);

        if (!income.getAccountId().equals(command.accountId())) {
            accountDomainService.requireActiveAccount(command.accountId());
            income.changeAccount(command.accountId());
        }
        income.changeDescription(command.description());
        income.changeAmount(command.amount());
        income.changeReceivedAt(command.receivedAt());
        income.touch(now);

        Income saved = incomeRepositoryPort.save(income);
        auditPort.record(AppConstants.ENTITY_INCOME, id, AuditAction.ATUALIZACAO, oldValue, saved);
        log.info("Income updated id={}", id);
        return saved;
    }
}
