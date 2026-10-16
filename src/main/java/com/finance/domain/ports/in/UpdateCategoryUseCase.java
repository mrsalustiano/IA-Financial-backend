package com.finance.domain.ports.in;

import com.finance.application.command.UpdateCategoryCommand;
import com.finance.domain.model.Category;

public interface UpdateCategoryUseCase {

    Category handle(String id, UpdateCategoryCommand command);
}
