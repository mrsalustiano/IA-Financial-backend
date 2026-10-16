package com.finance.application.usecase;

import com.finance.application.command.UpdateIncomeCommand;
import com.finance.domain.enums.AccountType;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Account;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.AccountDomainService;
import com.finance.shared.exception.NotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateIncomeUseCaseImplTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private AccountDomainService accountDomainService;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private UpdateIncomeUseCaseImpl useCase;

    @Test
    void updatesFieldsAndAudits() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Old", new BigDecimal("100"),
            LocalDate.of(2026, 10, 1), Instant.parse("2026-01-01T00:00:00Z"));
        when(incomeRepositoryPort.findById("inc-1")).thenReturn(Optional.of(income));
        when(incomeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        Income result = useCase.handle("inc-1", new UpdateIncomeCommand("acc-1", "New",
            new BigDecimal("250.75"), LocalDate.of(2026, 10, 2)));

        assertThat(result.getDescription()).isEqualTo("New");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("250.75"));
        assertThat(result.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(result.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        verify(accountDomainService, never()).requireActiveAccount(any());
        verify(auditPort).record(org.mockito.ArgumentMatchers.eq("Income"),
            org.mockito.ArgumentMatchers.eq("inc-1"), eq(AuditAction.ATUALIZACAO), any(), any());
    }

    @Test
    void revalidatesAccountWhenAccountChanges() {
        Account newAccount = Account.create("acc-2", "LOCAL_USER", "Other", null, AccountType.DINHEIRO, true, Instant.now());
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Old", new BigDecimal("100"),
            LocalDate.of(2026, 10, 1), Instant.now());
        when(incomeRepositoryPort.findById("inc-1")).thenReturn(Optional.of(income));
        when(accountDomainService.requireActiveAccount("acc-2")).thenReturn(newAccount);
        when(incomeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Income result = useCase.handle("inc-1", new UpdateIncomeCommand("acc-2", "Same amount",
            new BigDecimal("100"), LocalDate.of(2026, 10, 1)));

        assertThat(result.getAccountId()).isEqualTo("acc-2");
        verify(accountDomainService).requireActiveAccount("acc-2");
    }

    @Test
    void throwsNotFoundWhenIncomeMissing() {
        when(incomeRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing", new UpdateIncomeCommand("acc-1", "New",
            new BigDecimal("10"), LocalDate.of(2026, 10, 1))))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void rejectsZeroAmountOnUpdate() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Old", new BigDecimal("100"),
            LocalDate.of(2026, 10, 1), Instant.now());
        when(incomeRepositoryPort.findById("inc-1")).thenReturn(Optional.of(income));

        assertThatThrownBy(() -> useCase.handle("inc-1", new UpdateIncomeCommand("acc-1", "Zero",
            BigDecimal.ZERO, LocalDate.of(2026, 10, 1))))
            .isInstanceOf(com.finance.shared.exception.ValidationException.class);

        verify(incomeRepositoryPort, never()).save(any());
    }
}
