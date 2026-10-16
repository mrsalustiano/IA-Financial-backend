package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteIncomeUseCaseImplTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private DeleteIncomeUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Income> incomeCaptor;

    @Test
    void softDeletesIncomeAndAudits() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 10, 1), Instant.now());
        when(incomeRepositoryPort.findById("inc-1")).thenReturn(Optional.of(income));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        useCase.handle("inc-1");

        verify(incomeRepositoryPort).save(incomeCaptor.capture());
        assertThat(incomeCaptor.getValue().isDeleted()).isTrue();
        assertThat(incomeCaptor.getValue().getDeletedAt()).isNotNull();
        verify(auditPort).record(eq("Income"), eq("inc-1"), eq(AuditAction.EXCLUSAO), eq("snapshot"), isNull());
    }

    @Test
    void throwsNotFoundWhenIncomeMissing() {
        when(incomeRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing"))
            .isInstanceOf(NotFoundException.class);

        verify(incomeRepositoryPort, org.mockito.Mockito.never()).save(any());
    }
}
