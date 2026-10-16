package com.finance.domain.model;

import java.time.Instant;

public abstract class BaseEntity {

    protected String id;
    protected String userId;
    protected boolean deleted;
    protected Instant deletedAt;
    protected Instant createdAt;
    protected Instant updatedAt;

    protected BaseEntity(String id, String userId, boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markDeleted(Instant at) {
        this.deleted = true;
        this.deletedAt = at;
        this.updatedAt = at;
    }

    public void touch(Instant at) {
        this.updatedAt = at;
    }
}
