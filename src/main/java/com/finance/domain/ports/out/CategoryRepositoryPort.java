package com.finance.domain.ports.out;

import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Category;

import java.util.Optional;

public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findById(String id);

    boolean existsByName(String name, String excludeId);

    PageResult<Category> search(CategorySearchQuery query);
}
