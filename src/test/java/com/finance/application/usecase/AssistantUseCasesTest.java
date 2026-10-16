package com.finance.application.usecase;

import com.finance.application.command.AskAssistantCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.AssistantMessageCreatedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.model.AiResponse;
import com.finance.domain.model.AssistantMessage;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AssistantMessageRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.domain.services.FinancialPlanningDomainService;
import com.finance.shared.exception.ExternalServiceException;
import com.finance.shared.exception.NotFoundException;
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
class AssistantUseCasesTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private AssistantMessageRepositoryPort messageRepositoryPort;

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
    private AskAssistantUseCaseImpl askUseCase;

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void answersQuestionUsingFinancialContextAndPersistsHistory() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Conta", null,
            com.finance.domain.enums.AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(account), 0, 500, 1, 1));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(Income.create("inc-1", "LOCAL_USER", "acc-1", "Salario",
                new BigDecimal("3000"), LocalDate.of(2026, 9, 5), Instant.now())), 0, 500, 1, 1));
        when(aiProviderPort.complete(any())).thenReturn(new AiResponse("Seu saldo de setembro e 3000", "ollama",
            "llama3"));
        when(messageRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AssistantMessage message = askUseCase.handle(new AskAssistantCommand("qual meu saldo?"));

        assertThat(message.getQuestion()).isEqualTo("qual meu saldo?");
        assertThat(message.getAnswer()).contains("3000");
        assertThat(message.getProvider()).isEqualTo("ollama");
        ArgumentCaptor<AssistantMessageCreatedEvent> eventCaptor =
            ArgumentCaptor.forClass(AssistantMessageCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().messageId()).isEqualTo(message.getId());
        verify(auditPort).record(eq("AssistantMessage"), eq(message.getId()), eq(AuditAction.CRIACAO), isNull(),
            eq(message));
    }

    @Test
    void propagatesAiFailureWithoutSaving() {
        when(accountRepositoryPort.search(any(AccountSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        when(aiProviderPort.complete(any())).thenThrow(new ExternalServiceException("Ollama indisponivel"));

        assertThatThrownBy(() -> askUseCase.handle(new AskAssistantCommand("pergunta")))
            .isInstanceOf(ExternalServiceException.class);
        verify(messageRepositoryPort, never()).save(any());
    }

    @Test
    void getsAndListsMessages() {
        GetAssistantMessageByIdUseCaseImpl getUseCase = new GetAssistantMessageByIdUseCaseImpl(messageRepositoryPort);
        ListAssistantMessagesUseCaseImpl listUseCase = new ListAssistantMessagesUseCaseImpl(messageRepositoryPort);
        AssistantMessage message = AssistantMessage.create("am-1", "LOCAL_USER", "q", "a", "ollama", "llama3",
            Instant.now());
        when(messageRepositoryPort.findById("am-1")).thenReturn(Optional.of(message));
        assertThat(getUseCase.handle("am-1")).isSameAs(message);
        when(messageRepositoryPort.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> getUseCase.handle("missing")).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Mensagem não encontrada");
        when(messageRepositoryPort.search(0, 20)).thenReturn(new PageResult<>(List.of(message), 0, 20, 1, 1));
        assertThat(listUseCase.handle(0, 20).content()).containsExactly(message);
    }
}
