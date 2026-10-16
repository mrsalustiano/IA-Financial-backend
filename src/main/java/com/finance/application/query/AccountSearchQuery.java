package com.finance.application.query;

import org.springframework.data.domain.Pageable;

import com.finance.domain.enums.AccountType;

public record AccountSearchQuery(AccountType type, Boolean active, Pageable pageable) {
}
