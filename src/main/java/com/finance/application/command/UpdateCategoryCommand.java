package com.finance.application.command;

import com.finance.domain.enums.CategoryClassification;

public record UpdateCategoryCommand(String name, String description, CategoryClassification classification,
                                    Boolean active) {
}
