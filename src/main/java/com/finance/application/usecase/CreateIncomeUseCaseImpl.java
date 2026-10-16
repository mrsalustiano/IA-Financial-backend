package com.finance.application.usecase;

import com.finance.application.command.CreateIncomeCommand;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.IncomeCreatedEvent;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.CreateIncomeUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.AccountDomainService;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateIncomeUseCaseImpl implements CreateIncomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateIncomeUseCaseImpl.class);

    private final IncomeRepositoryPort incomeRepositoryPort;
    private final AccountDomainService accountDomainService;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateIncomeUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort,
                                   AccountDomainService accountDomainService, AuditPort auditPort,
                                   EventPublisherPort eventPublisherPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.accountDomainService = accountDomainService;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public Income handle(CreateIncomeCommand command) {
        log.info("Creating income accountId={}", command.accountId());
        Instant now = Instant.now();
        accountDomainService.requireActiveAccount(command.accountId());
        Income income = Income.create(Identifiers.newId(), AppConstants.LOCAL_USER_ID, command.accountId(),
            command.description(), command.amount(), command.receivedAt(), now);
        Income saved = incomeRepositoryPort.save(income);
        auditPort.record(AppConstants.ENTITY_INCOME, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new IncomeCreatedEvent(saved.getId(), saved.getUserId(), saved.getAccountId(), now));
        log.info("Income created id={}", saved.getId());
        return saved;
    }
}
