package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.DeleteIncomeUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeleteIncomeUseCaseImpl implements DeleteIncomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteIncomeUseCaseImpl.class);

    private final IncomeRepositoryPort incomeRepositoryPort;
    private final AuditPort auditPort;

    public DeleteIncomeUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort, AuditPort auditPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public void handle(String id) {
        log.info("Deleting income id={}", id);
        Income income = incomeRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Receita não encontrada: " + id));
        Object oldValue = auditPort.snapshot(income);
        income.markDeleted(Instant.now());
        incomeRepositoryPort.save(income);
        auditPort.record(AppConstants.ENTITY_INCOME, id, AuditAction.EXCLUSAO, oldValue, null);
        log.info("Income deleted id={}", id);
    }
}
