package com.finance.application.usecase;

import com.finance.application.command.CreateCategoryCommand;
import com.finance.application.command.UpdateCategoryCommand;
import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.application.validator.CategoryValidator;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.enums.CategoryClassification;
import com.finance.domain.events.CategoryCreatedEvent;
import com.finance.domain.model.Category;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.shared.exception.ConflictException;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.finance.domain.ports.out.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class CategoryUseCasesTest {

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private CategoryValidator categoryValidator;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private CreateCategoryUseCaseImpl createUseCase;

    @InjectMocks
    private UpdateCategoryUseCaseImpl updateUseCase;

    @InjectMocks
    private DeleteCategoryUseCaseImpl deleteUseCase;

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void createsCategoryAuditsAndPublishesEvent() {
        when(categoryRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = createUseCase.handle(new CreateCategoryCommand("Mercado", "Sup",
            CategoryClassification.DESPESA, true));

        assertThat(result.getName()).isEqualTo("Mercado");
        assertThat(result.isActive()).isTrue();
        verify(auditPort).record(eq("Category"), eq(result.getId()), eq(AuditAction.CRIACAO), isNull(), eq(result));
        ArgumentCaptor<CategoryCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CategoryCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().categoryId()).isEqualTo(result.getId());
    }

    @Test
    void createsInactiveCategoryWhenActiveFalse() {
        when(categoryRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Category result = createUseCase.handle(new CreateCategoryCommand("Lazer", null,
            CategoryClassification.DESPESA, false));
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updatesCategoryWithAudit() {
        Category existing = Category.create("cat-1", "LOCAL_USER", "Old", null, CategoryClassification.DESPESA,
            true, Instant.parse("2026-01-01T00:00:00Z"));
        when(categoryRepositoryPort.findById("cat-1")).thenReturn(Optional.of(existing));
        when(categoryRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        Category result = updateUseCase.handle("cat-1", new UpdateCategoryCommand("New", "d",
            CategoryClassification.RECEITA, false));

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getClassification()).isEqualTo(CategoryClassification.RECEITA);
        assertThat(result.isActive()).isFalse();
        assertThat(result.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        verify(categoryValidator).ensureNameAvailable("New", "cat-1");
        verify(auditPort).record(eq("Category"), eq("cat-1"), eq(AuditAction.ATUALIZACAO), eq("snapshot"), eq(result));
    }

    @Test
    void updateUnknownCategoryThrows404() {
        when(categoryRepositoryPort.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> updateUseCase.handle("missing",
            new UpdateCategoryCommand("New", null, CategoryClassification.DESPESA, true)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Categoria não encontrada");
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    void softDeletesCategoryWithAudit() {
        Category existing = Category.create("cat-1", "LOCAL_USER", "Doomed", null,
            CategoryClassification.DESPESA, true, Instant.now());
        when(categoryRepositoryPort.findById("cat-1")).thenReturn(Optional.of(existing));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        deleteUseCase.handle("cat-1");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(auditPort).record(eq("Category"), eq("cat-1"), eq(AuditAction.EXCLUSAO), eq("snapshot"), isNull());
    }

    @Test
    void deleteUnknownCategoryThrows404() {
        when(categoryRepositoryPort.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> deleteUseCase.handle("missing")).isInstanceOf(NotFoundException.class);
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    void validatorRejectsDuplicatedName() {
        when(categoryRepositoryPort.existsByName("Mercado", null)).thenReturn(true);
        CategoryValidator validator = new CategoryValidator(categoryRepositoryPort);
        assertThatThrownBy(() -> validator.ensureNameAvailable("Mercado", null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Já existe uma categoria com o nome");
    }

    @Test
    void listsCategoriesThroughRepository() {
        ListCategoriesUseCaseImpl listUseCase = new ListCategoriesUseCaseImpl(categoryRepositoryPort);
        GetCategoryByIdUseCaseCaseSupport();
        CategorySearchQuery query = new CategorySearchQuery(CategoryClassification.DESPESA, true, null);
        PageResult<Category> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(categoryRepositoryPort.search(query)).thenReturn(expected);
        assertThat(listUseCase.handle(query)).isSameAs(expected);
    }

    private void GetCategoryByIdUseCaseCaseSupport() {
        // noop to keep test class compact
    }
}
