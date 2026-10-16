package com.finance.application.command;

import com.finance.domain.enums.CategoryClassification;

public record CreateCategoryCommand(String name, String description, CategoryClassification classification,
                                    Boolean active) {
}
