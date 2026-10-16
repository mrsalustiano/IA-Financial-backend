package com.finance.domain.model;

import com.finance.domain.enums.CategoryClassification;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createsCategoryWithDefaults() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Category category = Category.create("cat-1", "LOCAL_USER", "Mercado", "Supermercado",
            CategoryClassification.DESPESA, true, now);
        assertThat(category.getId()).isEqualTo("cat-1");
        assertThat(category.getName()).isEqualTo("Mercado");
        assertThat(category.getClassification()).isEqualTo(CategoryClassification.DESPESA);
        assertThat(category.isActive()).isTrue();
        assertThat(category.isDeleted()).isFalse();
        assertThat(category.getCreatedAt()).isEqualTo(now);
        assertThat(category.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void rejectsNullClassification() {
        assertThatThrownBy(() -> Category.create("cat-1", "LOCAL_USER", "Mercado", null, null, true, Instant.now()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Classificação da categoria é obrigatória");
    }

    @Test
    void changesFields() {
        Category category = Category.create("cat-1", "LOCAL_USER", "Old", null, CategoryClassification.DESPESA,
            true, Instant.now());
        category.rename("New");
        category.describe("desc");
        category.changeClassification(CategoryClassification.RECEITA);
        category.deactivate();
        assertThat(category.getName()).isEqualTo("New");
        assertThat(category.getDescription()).isEqualTo("desc");
        assertThat(category.getClassification()).isEqualTo(CategoryClassification.RECEITA);
        assertThat(category.isActive()).isFalse();
        assertThatThrownBy(() -> category.changeClassification(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void softDeletes() {
        Category category = Category.create("cat-1", "LOCAL_USER", "Mercado", null,
            CategoryClassification.DESPESA, true, Instant.now());
        category.markDeleted(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(category.isDeleted()).isTrue();
        assertThat(category.getDeletedAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void equalityById() {
        Category a = Category.create("cat-1", "LOCAL_USER", "A", null, CategoryClassification.DESPESA, true,
            Instant.now());
        Category b = Category.create("cat-1", "LOCAL_USER", "B", null, CategoryClassification.RECEITA, false,
            Instant.now());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
