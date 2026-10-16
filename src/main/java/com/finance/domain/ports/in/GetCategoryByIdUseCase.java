package com.finance.domain.ports.in;

import com.finance.domain.model.Category;

public interface GetCategoryByIdUseCase {

    Category handle(String id);
}
