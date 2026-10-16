package com.finance.adapters.out.mongodb;

import com.finance.domain.enums.CategoryClassification;
import com.finance.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryPersistenceMapperTest {

    private final CategoryPersistenceMapper mapper = Mappers.getMapper(CategoryPersistenceMapper.class);

    @Test
    void mapsBothWays() {
        Instant now = Instant.now();
        Category category = Category.create("cat-1", "LOCAL_USER", "Mercado", "desc",
            CategoryClassification.DESPESA, true, now);
        CategoryDocument document = mapper.toDocument(category);
        assertThat(document.getId()).isEqualTo("cat-1");
        assertThat(document.getClassification()).isEqualTo(CategoryClassification.DESPESA);
        Category domain = mapper.toDomain(document);
        assertThat(domain.getName()).isEqualTo("Mercado");
        assertThat(domain.isActive()).isTrue();
    }
}
