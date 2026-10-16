package com.finance.application.usecase;

import com.finance.application.command.UpdateCategoryCommand;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Category;
import com.finance.domain.ports.in.UpdateCategoryUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.application.validator.CategoryValidator;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateCategoryUseCaseImpl implements UpdateCategoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateCategoryUseCaseImpl.class);

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final CategoryValidator categoryValidator;
    private final AuditPort auditPort;

    public UpdateCategoryUseCaseImpl(CategoryRepositoryPort categoryRepositoryPort, CategoryValidator categoryValidator,
                                     AuditPort auditPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.categoryValidator = categoryValidator;
        this.auditPort = auditPort;
    }

    @Override
    public Category handle(String id, UpdateCategoryCommand command) {
        log.info("Updating category id={}", id);
        Instant now = Instant.now();
        Category category = categoryRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Categoria não encontrada: " + id));
        Object oldValue = auditPort.snapshot(category);

        categoryValidator.ensureNameAvailable(command.name(), id);
        category.rename(command.name());
        category.describe(command.description());
        category.changeClassification(command.classification());
        if (Boolean.TRUE.equals(command.active())) {
            category.activate();
        } else {
            category.deactivate();
        }
        category.touch(now);

        Category saved = categoryRepositoryPort.save(category);
        auditPort.record(AppConstants.ENTITY_CATEGORY, id, AuditAction.ATUALIZACAO, oldValue, saved);
        log.info("Category updated id={}", id);
        return saved;
    }
}
