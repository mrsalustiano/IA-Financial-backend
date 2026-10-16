package com.finance.application.usecase;

import com.finance.application.command.CreateFinancialPlanCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.FinancialPlanCreatedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.model.AiResponse;
import com.finance.domain.model.FinancialPlan;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.FinancialPlanRepositoryPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.domain.services.FinancialPlanningDomainService;
import com.finance.shared.exception.ExternalServiceException;
import com.finance.shared.exception.NotFoundException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class FinancialPlanUseCasesTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private FinancialPlanRepositoryPort planRepositoryPort;

    @Mock
    private AiProviderPort aiProviderPort;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Spy
    private FinancialAnalysisDomainService analysisDomainService = new FinancialAnalysisDomainService();

    @Spy
    private FinancialPlanningDomainService planningDomainService = new FinancialPlanningDomainService();

    @InjectMocks
    private CreateFinancialPlanUseCaseImpl createUseCase;

    private void givenFinancialData() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Conta", null,
            com.finance.domain.enums.AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(account), 0, 500, 1, 1));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(Income.create("inc-1", "LOCAL_USER", "acc-1", "Salario",
                new BigDecimal("3000"), LocalDate.of(2026, 9, 5), Instant.now())), 0, 500, 1, 1));
        org.mockito.Mockito.lenient().when(planRepositoryPort.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void createsPlanWithAiResponseHistoryAuditAndEvent() {
        givenFinancialData();
        when(aiProviderPort.complete(any())).thenReturn(new AiResponse(
            "Meta viavel em 6 meses.\n- Guardar 500 por mes\n- Cortar gastos supérfluos", "ollama", "llama3"));

        FinancialPlan plan = createUseCase.handle(new CreateFinancialPlanCommand("juntar 10 mil",
            new BigDecimal("10000"), LocalDate.of(2026, 12, 31)));

        assertThat(plan.getGoal()).isEqualTo("juntar 10 mil");
        assertThat(plan.getRecommendations()).containsExactly("Guardar 500 por mes", "Cortar gastos supérfluos");
        assertThat(plan.getPrompt()).contains("Meta do usuario: juntar 10 mil");
        assertThat(plan.getProvider()).isEqualTo("ollama");
        ArgumentCaptor<FinancialPlanCreatedEvent> eventCaptor = ArgumentCaptor.forClass(FinancialPlanCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().planId()).isEqualTo(plan.getId());
        verify(auditPort).record(eq("FinancialPlan"), eq(plan.getId()), eq(AuditAction.CRIACAO), isNull(), eq(plan));
    }

    @Test
    void rejectsPlanWithoutIncomes() {
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));

        assertThatThrownBy(() -> createUseCase.handle(new CreateFinancialPlanCommand("meta", null, null)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Não há receitas suficientes");
        verify(aiProviderPort, never()).complete(any());
        verify(planRepositoryPort, never()).save(any());
    }

    @Test
    void propagatesAiFailureWithoutSaving() {
        givenFinancialData();
        when(aiProviderPort.complete(any())).thenThrow(new ExternalServiceException("Ollama retornou status de erro: 500"));

        assertThatThrownBy(() -> createUseCase.handle(new CreateFinancialPlanCommand("meta impossivel", null, null)))
            .isInstanceOf(ExternalServiceException.class);
        verify(planRepositoryPort, never()).save(any());
        verify(auditPort, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void getsAndListsPlans() {
        GetFinancialPlanByIdUseCaseImpl getUseCase = new GetFinancialPlanByIdUseCaseImpl(planRepositoryPort);
        ListFinancialPlansUseCaseImpl listUseCase = new ListFinancialPlansUseCaseImpl(planRepositoryPort);
        FinancialPlan plan = FinancialPlan.create("fp-1", "LOCAL_USER", "meta", BigDecimal.TEN,
            LocalDate.of(2026, 12, 31), "p", "r", List.of("s"), "ollama", "llama3", Instant.now());
        when(planRepositoryPort.findById("fp-1")).thenReturn(Optional.of(plan));
        assertThat(getUseCase.handle("fp-1")).isSameAs(plan);
        when(planRepositoryPort.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> getUseCase.handle("missing")).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Plano financeiro não encontrado");
        when(planRepositoryPort.search(0, 20)).thenReturn(new PageResult<>(List.of(plan), 0, 20, 1, 1));
        assertThat(listUseCase.handle(0, 20).content()).containsExactly(plan);
    }
}
