package com.finance.application.usecase;

import com.finance.domain.model.DailyBalanceEntry;
import com.finance.domain.ports.out.DailyBalanceQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import com.finance.domain.ports.out.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetDailyBalanceUseCaseImplTest {

    @Mock
    private DailyBalanceQueryPort dailyBalanceQueryPort;

    @InjectMocks
    private GetDailyBalanceUseCaseImpl useCase;

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void accumulatesBalancesInOrder() {
        when(dailyBalanceQueryPort.findDailyBalances("LOCAL_USER", LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 3)))
            .thenReturn(List.of(
                new DailyBalanceEntry(LocalDate.of(2026, 10, 1), new BigDecimal("100"), BigDecimal.ZERO),
                new DailyBalanceEntry(LocalDate.of(2026, 10, 2), new BigDecimal("50"), BigDecimal.ZERO),
                new DailyBalanceEntry(LocalDate.of(2026, 10, 3), new BigDecimal("25"), BigDecimal.ZERO)));

        List<DailyBalanceEntry> result = useCase.handle(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).accumulated()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.get(1).accumulated()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(result.get(2).accumulated()).isEqualByComparingTo(new BigDecimal("175"));
        verifyNoMoreInteractions(dailyBalanceQueryPort);
    }

    @Test
    void returnsEmptyListWithoutMovements() {
        when(dailyBalanceQueryPort.findDailyBalances("LOCAL_USER", null, null)).thenReturn(List.of());
        assertThat(useCase.handle(null, null)).isEmpty();
    }
}
