package com.finance.application.usecase;

import com.finance.domain.model.Category;
import com.finance.domain.ports.in.GetCategoryByIdUseCase;
import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetCategoryByIdUseCaseImpl implements GetCategoryByIdUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public GetCategoryByIdUseCaseImpl(CategoryRepositoryPort categoryRepositoryPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
    }

    @Override
    public Category handle(String id) {
        return categoryRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Categoria não encontrada: " + id));
    }
}
