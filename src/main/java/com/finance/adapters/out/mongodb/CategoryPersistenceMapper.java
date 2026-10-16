package com.finance.adapters.out.mongodb;

import com.finance.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryPersistenceMapper {

    CategoryDocument toDocument(Category category);

    default Category toDomain(CategoryDocument document) {
        return Category.rehydrate(document.getId(), document.getUserId(), document.getName(),
            document.getDescription(), document.getClassification(), document.isActive(), document.isDeleted(),
            document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
