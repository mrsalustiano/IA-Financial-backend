package com.finance.domain.model;

import com.finance.domain.enums.AccountType;

import java.time.Instant;

public class Account extends BaseEntity {

    private String name;
    private String description;
    private AccountType type;
    private boolean active;

    private Account(String id, String userId, String name, String description, AccountType type, boolean active,
                    boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.name = name;
        this.description = description;
        this.type = type;
        this.active = active;
    }

    public static Account create(String id, String userId, String name, String description, AccountType type,
                                 boolean active, Instant now) {
        return new Account(id, userId, name, description, type, active, false, null, now, now);
    }

    public static Account rehydrate(String id, String userId, String name, String description, AccountType type,
                                    boolean active, boolean deleted, Instant deletedAt, Instant createdAt,
                                    Instant updatedAt) {
        return new Account(id, userId, name, description, type, active, deleted, deletedAt, createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void changeType(AccountType type) {
        this.type = type;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }
}
