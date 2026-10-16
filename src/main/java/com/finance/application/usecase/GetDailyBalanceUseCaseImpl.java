package com.finance.application.usecase;

import com.finance.domain.model.DailyBalanceEntry;
import com.finance.domain.ports.in.GetDailyBalanceUseCase;
import com.finance.domain.ports.out.DailyBalanceQueryPort;
import com.finance.shared.constants.AppConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GetDailyBalanceUseCaseImpl implements GetDailyBalanceUseCase {

    private final DailyBalanceQueryPort dailyBalanceQueryPort;

    public GetDailyBalanceUseCaseImpl(DailyBalanceQueryPort dailyBalanceQueryPort) {
        this.dailyBalanceQueryPort = dailyBalanceQueryPort;
    }

    @Override
    public List<DailyBalanceEntry> handle(LocalDate startDate, LocalDate endDate) {
        List<DailyBalanceEntry> entries = dailyBalanceQueryPort.findDailyBalances(AppConstants.LOCAL_USER_ID,
            startDate, endDate);
        List<DailyBalanceEntry> accumulated = new ArrayList<>(entries.size());
        BigDecimal running = BigDecimal.ZERO;
        for (DailyBalanceEntry entry : entries) {
            running = running.add(entry.balance());
            accumulated.add(new DailyBalanceEntry(entry.date(), entry.totalIncome(), entry.totalExpense(),
                running));
        }
        return accumulated;
    }
}
