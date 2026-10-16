package com.finance.domain.ports.in;

import com.finance.domain.model.MonthlyClosing;

import java.util.List;

public interface ListMonthlyClosingsUseCase {

    List<MonthlyClosing> handle();
}
