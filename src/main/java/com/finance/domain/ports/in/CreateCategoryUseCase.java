package com.finance.domain.ports.in;

import com.finance.application.command.CreateCategoryCommand;
import com.finance.domain.model.Category;

public interface CreateCategoryUseCase {

    Category handle(CreateCategoryCommand command);
}
