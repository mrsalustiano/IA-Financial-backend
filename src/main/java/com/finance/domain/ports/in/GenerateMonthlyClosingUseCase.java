package com.finance.domain.ports.in;

import com.finance.domain.model.MonthlyClosing;

public interface GenerateMonthlyClosingUseCase {

    MonthlyClosing handle(String referenceMonth);

    MonthlyClosing handleForUser(String userId, String referenceMonth);
}
