package com.finance.domain.ports.out;

import com.finance.domain.model.DailyBalanceEntry;

import java.time.LocalDate;
import java.util.List;

public interface DailyBalanceQueryPort {

    List<DailyBalanceEntry> findDailyBalances(String userId, LocalDate startDate, LocalDate endDate);
}
