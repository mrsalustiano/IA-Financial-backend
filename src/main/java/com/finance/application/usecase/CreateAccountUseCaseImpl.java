package com.finance.application.usecase;

import com.finance.application.command.CreateAccountCommand;
import com.finance.application.validator.AccountValidator;
import com.finance.domain.events.AccountCreatedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.ports.in.CreateAccountUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.enums.AuditAction;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import com.finance.domain.ports.out.CurrentUserPort;

@Service
public class CreateAccountUseCaseImpl implements CreateAccountUseCase {

    private final CurrentUserPort currentUserPort;

    private static final Logger log = LoggerFactory.getLogger(CreateAccountUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountValidator accountValidator;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateAccountUseCaseImpl(AccountRepositoryPort accountRepositoryPort, AccountValidator accountValidator,
                                    AuditPort auditPort, EventPublisherPort eventPublisherPort,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.accountValidator = accountValidator;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public Account handle(CreateAccountCommand command) {
        log.info("Creating account name={}", command.name());
        Instant now = Instant.now();
        accountValidator.ensureNameAvailable(command.name(), null);
        boolean active = command.active() == null || command.active();
        Account account = Account.create(Identifiers.newId(), currentUserPort.requireUserId(), command.name(),
            command.description(), command.type(), active, now);
        Account saved = accountRepositoryPort.save(account);
        auditPort.record(AppConstants.ENTITY_ACCOUNT, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new AccountCreatedEvent(saved.getId(), saved.getUserId(), now));
        log.info("Account created id={}", saved.getId());
        return saved;
    }
}
