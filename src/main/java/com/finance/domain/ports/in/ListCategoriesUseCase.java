package com.finance.domain.ports.in;

import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Category;

public interface ListCategoriesUseCase {

    PageResult<Category> handle(CategorySearchQuery query);
}
