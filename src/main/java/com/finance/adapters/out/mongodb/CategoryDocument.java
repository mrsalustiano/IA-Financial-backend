package com.finance.adapters.out.mongodb;

import com.finance.domain.enums.CategoryClassification;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "categories")
public class CategoryDocument {

    @Id
    private String id;
    private String userId;
    private String name;
    private String description;
    private CategoryClassification classification;
    private boolean active;
    private boolean deleted;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public CategoryDocument() {
    }

    public CategoryDocument(String id, String userId, String name, String description,
                            CategoryClassification classification, boolean active, boolean deleted,
                            Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.classification = classification;
        this.active = active;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CategoryClassification getClassification() {
        return classification;
    }

    public void setClassification(CategoryClassification classification) {
        this.classification = classification;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
