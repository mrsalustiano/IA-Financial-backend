package com.finance.application.usecase;

import com.finance.application.command.CreateCategoryCommand;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.CategoryCreatedEvent;
import com.finance.domain.model.Category;
import com.finance.domain.ports.in.CreateCategoryUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.application.validator.CategoryValidator;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import com.finance.domain.ports.out.CurrentUserPort;

@Service
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {

    private final CurrentUserPort currentUserPort;

    private static final Logger log = LoggerFactory.getLogger(CreateCategoryUseCaseImpl.class);

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final CategoryValidator categoryValidator;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateCategoryUseCaseImpl(CategoryRepositoryPort categoryRepositoryPort,
                                     CategoryValidator categoryValidator, AuditPort auditPort,
                                     EventPublisherPort eventPublisherPort,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.categoryValidator = categoryValidator;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public Category handle(CreateCategoryCommand command) {
        log.info("Creating category name={}", command.name());
        Instant now = Instant.now();
        categoryValidator.ensureNameAvailable(command.name(), null);
        boolean active = !Boolean.FALSE.equals(command.active());
        Category category = Category.create(Identifiers.newId(), currentUserPort.requireUserId(), command.name(),
            command.description(), command.classification(), active, now);
        Category saved = categoryRepositoryPort.save(category);
        auditPort.record(AppConstants.ENTITY_CATEGORY, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new CategoryCreatedEvent(saved.getId(), saved.getUserId(), now));
        log.info("Category created id={}", saved.getId());
        return saved;
    }
}
