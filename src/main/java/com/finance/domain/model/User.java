package com.finance.domain.model;

import java.time.Instant;
import java.util.Objects;

public class User extends BaseEntity {

    private String email;
    private String cpf;
    private String passwordHash;
    private boolean active;

    private User(String id, String userId, String email, String cpf, String passwordHash, boolean active,
                 boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.email = email;
        this.cpf = cpf;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public static User create(String id, String email, String cpf, String passwordHash, boolean active, Instant now) {
        return new User(id, id, email, cpf, passwordHash, active, false, null, now, now);
    }

    public static User rehydrate(String id, String email, String cpf, String passwordHash, boolean active,
                                 boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        return new User(id, id, email, cpf, passwordHash, active, deleted, deletedAt, createdAt, updatedAt);
    }

    public void updateProfile(String newEmail, String newCpf, Instant now) {
        this.email = newEmail;
        this.cpf = newCpf;
        this.updatedAt = now;
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
        this.updatedAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }
}
