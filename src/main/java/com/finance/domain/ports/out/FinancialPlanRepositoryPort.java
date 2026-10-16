package com.finance.domain.ports.out;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialPlan;

import java.util.Optional;

public interface FinancialPlanRepositoryPort {

    FinancialPlan save(FinancialPlan plan);

    Optional<FinancialPlan> findById(String id);

    PageResult<FinancialPlan> search(int page, int size);
}
