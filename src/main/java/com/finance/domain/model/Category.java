package com.finance.domain.model;

import com.finance.domain.enums.CategoryClassification;
import com.finance.shared.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;

public class Category extends BaseEntity {

    private String name;
    private String description;
    private CategoryClassification classification;
    private boolean active;

    private Category(String id, String userId, String name, String description,
                     CategoryClassification classification, boolean active, boolean deleted,
                     Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.name = name;
        this.description = description;
        this.classification = classification;
        this.active = active;
    }

    public static Category create(String id, String userId, String name, String description,
                                  CategoryClassification classification, boolean active, Instant now) {
        validateClassification(classification);
        return new Category(id, userId, name, description, classification, active, false, null, now, now);
    }

    public static Category rehydrate(String id, String userId, String name, String description,
                                     CategoryClassification classification, boolean active, boolean deleted,
                                     Instant deletedAt, Instant createdAt, Instant updatedAt) {
        return new Category(id, userId, name, description, classification, active, deleted, deletedAt,
            createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void changeClassification(CategoryClassification classification) {
        validateClassification(classification);
        this.classification = classification;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static void validateClassification(CategoryClassification classification) {
        if (classification == null) {
            throw new ValidationException("Classificação da categoria é obrigatória");
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CategoryClassification getClassification() {
        return classification;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category category)) {
            return false;
        }
        return id != null && id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
