package com.finance.application.usecase;

import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Category;
import com.finance.domain.ports.in.ListCategoriesUseCase;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public ListCategoriesUseCaseImpl(CategoryRepositoryPort categoryRepositoryPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
    }

    @Override
    public PageResult<Category> handle(CategorySearchQuery query) {
        return categoryRepositoryPort.search(query);
    }
}
