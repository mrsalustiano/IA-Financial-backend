package com.finance.domain.ports.in;

import com.finance.domain.model.DailyBalanceEntry;

import java.time.LocalDate;
import java.util.List;

public interface GetDailyBalanceUseCase {

    List<DailyBalanceEntry> handle(LocalDate startDate, LocalDate endDate);
}
