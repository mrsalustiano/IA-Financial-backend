package com.finance.domain.ports.out;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;

import java.util.Optional;

public interface IncomeRepositoryPort {

    Income save(Income income);

    Optional<Income> findById(String id);

    PageResult<Income> search(IncomeSearchQuery query);
}
