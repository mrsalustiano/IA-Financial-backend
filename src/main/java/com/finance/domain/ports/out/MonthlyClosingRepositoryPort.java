package com.finance.domain.ports.out;

import com.finance.domain.model.MonthlyClosing;

import java.util.List;
import java.util.Optional;

public interface MonthlyClosingRepositoryPort {

    MonthlyClosing save(MonthlyClosing closing);

    Optional<MonthlyClosing> findByIdAndUser(String userId, String id);

    Optional<MonthlyClosing> findByUserAndReferenceMonth(String userId, String referenceMonth);

    List<MonthlyClosing> findAllByUser(String userId);
}
