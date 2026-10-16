package com.finance.application.usecase;

import com.finance.domain.model.Income;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIncomeByIdUseCaseImplTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @InjectMocks
    private GetIncomeByIdUseCaseImpl useCase;

    @Test
    void returnsIncomeWhenFound() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 10, 1), Instant.now());
        when(incomeRepositoryPort.findById("inc-1")).thenReturn(Optional.of(income));

        Income result = useCase.handle("inc-1");

        assertThat(result).isSameAs(income);
    }

    @Test
    void throwsNotFoundWhenMissing() {
        when(incomeRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing"))
            .isInstanceOf(NotFoundException.class);
    }
}
