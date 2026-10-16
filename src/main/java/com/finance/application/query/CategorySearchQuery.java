package com.finance.application.query;

import com.finance.domain.enums.CategoryClassification;
import org.springframework.data.domain.Pageable;

public record CategorySearchQuery(CategoryClassification classification, Boolean active, Pageable pageable) {
}
