package com.finance.application.query;

import org.springframework.data.domain.Pageable;

public record UserSearchQuery(Boolean active, Pageable pageable) {
}
