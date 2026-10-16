package com.finance.application.validator;

import com.finance.domain.ports.out.CategoryRepositoryPort;
import com.finance.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

@Component
public class CategoryValidator {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public CategoryValidator(CategoryRepositoryPort categoryRepositoryPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
    }

    public void ensureNameAvailable(String name, String excludeId) {
        if (categoryRepositoryPort.existsByName(name, excludeId)) {
            throw new ConflictException("Já existe uma categoria com o nome: " + name);
        }
    }
}
