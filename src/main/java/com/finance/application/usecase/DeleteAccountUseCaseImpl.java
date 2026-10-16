package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Account;
import com.finance.domain.ports.in.DeleteAccountUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccountUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final AuditPort auditPort;

    public DeleteAccountUseCaseImpl(AccountRepositoryPort accountRepositoryPort, AuditPort auditPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public void handle(String id) {
        log.info("Deleting account id={}", id);
        Account account = accountRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Conta não encontrada: " + id));
        Object oldValue = auditPort.snapshot(account);
        account.markDeleted(Instant.now());
        accountRepositoryPort.save(account);
        auditPort.record(AppConstants.ENTITY_ACCOUNT, id, AuditAction.EXCLUSAO, oldValue, null);
        log.info("Account deleted id={}", id);
    }
}
