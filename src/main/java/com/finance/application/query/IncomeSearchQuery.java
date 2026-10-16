package com.finance.application.query;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public record IncomeSearchQuery(String accountId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
}
