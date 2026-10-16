package com.finance.domain.model;

import com.finance.domain.enums.AuditAction;

import java.time.Instant;

public class AuditEntry {

    private final String id;
    private final String entityType;
    private final String entityId;
    private final AuditAction action;
    private final Object oldValue;
    private final Object newValue;
    private final Instant createdAt;

    public AuditEntry(String id, String entityType, String entityId, AuditAction action,
                      Object oldValue, Object newValue, Instant createdAt) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
