package com.finance.domain.ports.in;

import com.finance.domain.model.Income;

public interface GetIncomeByIdUseCase {

    Income handle(String id);
}
