package com.finance.application.usecase;

import com.finance.application.command.CreateIncomeCommand;
import com.finance.domain.enums.AccountType;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.IncomeCreatedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.AccountDomainService;
import com.finance.shared.exception.BusinessException;
import com.finance.shared.exception.NotFoundException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
class CreateIncomeUseCaseImplTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private AccountDomainService accountDomainService;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private CreateIncomeUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Income> incomeCaptor;

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void createsIncomeForActiveAccount() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountDomainService.requireActiveAccount("acc-1")).thenReturn(account);
        when(incomeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Income result = useCase.handle(new CreateIncomeCommand("acc-1", "Salary",
            new BigDecimal("1000.50"), LocalDate.of(2026, 10, 5)));

        verify(incomeRepositoryPort).save(incomeCaptor.capture());
        Income saved = incomeCaptor.getValue();
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(saved.getAccountId()).isEqualTo("acc-1");
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
        assertThat(saved.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));

        verify(auditPort).record(eq("Income"), eq(saved.getId()), eq(AuditAction.CRIACAO), isNull(), eq(saved));
        ArgumentCaptor<IncomeCreatedEvent> eventCaptor = ArgumentCaptor.forClass(IncomeCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(saved.getId());
        assertThat(result).isSameAs(saved);
    }

    @Test
    void rejectsNonPositiveAmount() {
        when(accountDomainService.requireActiveAccount("acc-1")).thenReturn(
            Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true, Instant.now()));

        assertThatThrownBy(() -> useCase.handle(new CreateIncomeCommand("acc-1", "Bad",
            BigDecimal.ZERO, LocalDate.of(2026, 10, 5))))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("maior que zero");

        verify(incomeRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountDomainService.requireActiveAccount("ghost"))
            .thenThrow(new NotFoundException("Conta não encontrada: ghost"));

        assertThatThrownBy(() -> useCase.handle(new CreateIncomeCommand("ghost", "Orphan",
            new BigDecimal("10"), LocalDate.of(2026, 10, 5))))
            .isInstanceOf(NotFoundException.class);

        verify(incomeRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsInactiveAccount() {
        when(accountDomainService.requireActiveAccount("acc-1"))
            .thenThrow(new BusinessException("Conta inativa: Main"));

        assertThatThrownBy(() -> useCase.handle(new CreateIncomeCommand("acc-1", "Blocked",
            new BigDecimal("10"), LocalDate.of(2026, 10, 5))))
            .isInstanceOf(BusinessException.class);

        verify(incomeRepositoryPort, never()).save(any());
    }
}
