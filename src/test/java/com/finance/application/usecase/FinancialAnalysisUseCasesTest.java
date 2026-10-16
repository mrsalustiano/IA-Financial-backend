package com.finance.application.usecase;

import com.finance.application.command.GenerateFinancialAnalysisCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.FinancialAnalysisGeneratedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.model.AiResponse;
import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.FinancialAnalysisRepositoryPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.shared.exception.ExternalServiceException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

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

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisUseCasesTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private FinancialAnalysisRepositoryPort analysisRepositoryPort;

    @Mock
    private AiProviderPort aiProviderPort;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @org.mockito.Spy
    private FinancialAnalysisDomainService analysisDomainService = new FinancialAnalysisDomainService();

    @InjectMocks
    private GenerateFinancialAnalysisUseCaseImpl generateUseCase;

    private Income income(String amount, LocalDate receivedAt) {
        return Income.create("inc-1", "LOCAL_USER", "acc-1", "Salario", new BigDecimal(amount), receivedAt,
            Instant.now());
    }

    private void givenFinancialData() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Conta", null,
            com.finance.domain.enums.AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(account), 0, 500, 1, 1));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(income("3000", LocalDate.of(2026, 9, 5)),
                income("3000", LocalDate.of(2026, 10, 5))), 0, 500, 2, 1));
        org.mockito.Mockito.lenient().when(analysisRepositoryPort.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generatesAnalysisWithHistoryAuditAndEvent() {
        givenFinancialData();
        when(aiProviderPort.complete(any())).thenReturn(new AiResponse(
            "Saude financeira ok.\n- Aumentar reserva\n- Reduzir gastos supérfluos", "ollama", "llama3"));

        FinancialAnalysis result = generateUseCase.handle(new GenerateFinancialAnalysisCommand("economia"));

        assertThat(result.getScore()).isEqualTo(36);
        assertThat(result.getRecommendations()).containsExactly("Aumentar reserva", "Reduzir gastos supérfluos");
        assertThat(result.getProvider()).isEqualTo("ollama");
        assertThat(result.getPrompt()).contains("Foco solicitado: economia");
        assertThat(result.getResponse()).contains("Saude financeira ok.");
        ArgumentCaptor<FinancialAnalysisGeneratedEvent> eventCaptor =
            ArgumentCaptor.forClass(FinancialAnalysisGeneratedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().analysisId()).isEqualTo(result.getId());
        assertThat(eventCaptor.getValue().score()).isEqualTo(36);
        verify(auditPort).record(eq("FinancialAnalysis"), eq(result.getId()), eq(AuditAction.CRIACAO), isNull(),
            eq(result));
    }

    @Test
    void rejectsAnalysisWithoutIncomes() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Conta", null,
            com.finance.domain.enums.AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(account), 0, 500, 1, 1));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));

        assertThatThrownBy(() -> generateUseCase.handle(new GenerateFinancialAnalysisCommand(null)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Não há receitas suficientes");
        verify(analysisRepositoryPort, never()).save(any());
        verify(aiProviderPort, never()).complete(any());
    }

    @Test
    void propagatesAiFailureWithoutSaving() {
        givenFinancialData();
        when(aiProviderPort.complete(any())).thenThrow(new ExternalServiceException("Ollama retornou status de erro: 500"));

        assertThatThrownBy(() -> generateUseCase.handle(new GenerateFinancialAnalysisCommand(null)))
            .isInstanceOf(ExternalServiceException.class);
        verify(analysisRepositoryPort, never()).save(any());
        verify(auditPort, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void getsAnalysisByIdOrThrows404() {
        GetFinancialAnalysisByIdUseCaseImpl getUseCase = new GetFinancialAnalysisByIdUseCaseImpl(analysisRepositoryPort);
        FinancialAnalysis analysis = FinancialAnalysis.create("fa-1", "LOCAL_USER", "p", "r", 50, List.of("a"),
            "ollama", "llama3", Instant.now());
        when(analysisRepositoryPort.findById("fa-1")).thenReturn(Optional.of(analysis));
        assertThat(getUseCase.handle("fa-1")).isSameAs(analysis);
        when(analysisRepositoryPort.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> getUseCase.handle("missing"))
            .isInstanceOf(com.finance.shared.exception.NotFoundException.class)
            .hasMessageContaining("Análise não encontrada");
    }

    @Test
    void listsAnalysesWithPagination() {
        ListFinancialAnalysesUseCaseImpl listUseCase = new ListFinancialAnalysesUseCaseImpl(analysisRepositoryPort);
        PageResult<FinancialAnalysis> expected = new PageResult<>(List.of(), 1, 20, 0, 0);
        when(analysisRepositoryPort.search(1, 20)).thenReturn(expected);
        assertThat(listUseCase.handle(1, 20)).isSameAs(expected);
    }
}
