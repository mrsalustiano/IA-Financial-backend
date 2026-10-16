package com.finance.domain.ports.in;

import com.finance.domain.model.MonthlyClosing;

public interface GetMonthlyClosingByIdUseCase {

    MonthlyClosing handle(String id);
}
