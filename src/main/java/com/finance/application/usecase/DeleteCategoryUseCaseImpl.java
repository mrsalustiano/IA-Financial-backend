package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Category;
import com.finance.domain.ports.in.DeleteCategoryUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeleteCategoryUseCaseImpl implements DeleteCategoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteCategoryUseCaseImpl.class);

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final AuditPort auditPort;

    public DeleteCategoryUseCaseImpl(CategoryRepositoryPort categoryRepositoryPort, AuditPort auditPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public void handle(String id) {
        log.info("Deleting category id={}", id);
        Category category = categoryRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Categoria não encontrada: " + id));
        Object oldValue = auditPort.snapshot(category);
        category.markDeleted(Instant.now());
        categoryRepositoryPort.save(category);
        auditPort.record(AppConstants.ENTITY_CATEGORY, id, AuditAction.EXCLUSAO, oldValue, null);
        log.info("Category deleted id={}", id);
    }
}
