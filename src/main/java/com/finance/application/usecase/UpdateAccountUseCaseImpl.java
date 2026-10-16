package com.finance.application.usecase;

import com.finance.application.command.UpdateAccountCommand;
import com.finance.application.validator.AccountValidator;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Account;
import com.finance.domain.ports.in.UpdateAccountUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateAccountUseCaseImpl implements UpdateAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateAccountUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountValidator accountValidator;
    private final AuditPort auditPort;

    public UpdateAccountUseCaseImpl(AccountRepositoryPort accountRepositoryPort, AccountValidator accountValidator,
                                    AuditPort auditPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.accountValidator = accountValidator;
        this.auditPort = auditPort;
    }

    @Override
    public Account handle(String id, UpdateAccountCommand command) {
        log.info("Updating account id={}", id);
        Instant now = Instant.now();
        Account account = accountRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Conta não encontrada: " + id));
        Object oldValue = auditPort.snapshot(account);

        accountValidator.ensureNameAvailable(command.name(), id);
        account.rename(command.name());
        account.describe(command.description());
        account.changeType(command.type());
        if (Boolean.TRUE.equals(command.active())) {
            account.activate();
        } else {
            account.deactivate();
        }
        account.touch(now);

        Account saved = accountRepositoryPort.save(account);
        auditPort.record(AppConstants.ENTITY_ACCOUNT, id, AuditAction.ATUALIZACAO, oldValue, saved);
        log.info("Account updated id={}", id);
        return saved;
    }
}
