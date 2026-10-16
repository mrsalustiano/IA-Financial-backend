package com.finance.application.usecase;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListIncomesUseCaseImplTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @InjectMocks
    private ListIncomesUseCaseImpl useCase;

    @Test
    void delegatesSearchToRepository() {
        IncomeSearchQuery query = new IncomeSearchQuery(null, "acc-1", LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30), PageRequest.of(0, 10));
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 9, 5), Instant.now());
        PageResult<Income> expected = new PageResult<>(List.of(income), 0, 10, 1, 1);
        when(incomeRepositoryPort.search(query)).thenReturn(expected);

        PageResult<Income> result = useCase.handle(query);

        assertThat(result).isSameAs(expected);
        assertThat(result.content()).hasSize(1);
    }
}
