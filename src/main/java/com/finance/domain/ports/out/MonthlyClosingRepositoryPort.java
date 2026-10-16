package com.finance.domain.ports.out;

import com.finance.domain.model.MonthlyClosing;

import java.util.Optional;

public interface MonthlyClosingRepositoryPort {

    MonthlyClosing save(MonthlyClosing closing);

    Optional<MonthlyClosing> findById(String id);

    Optional<MonthlyClosing> findByReferenceMonth(String referenceMonth);

    java.util.List<MonthlyClosing> findAll();
}
