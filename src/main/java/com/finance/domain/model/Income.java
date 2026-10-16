package com.finance.domain.model;

import com.finance.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Income extends BaseEntity {

    private String accountId;
    private String description;
    private BigDecimal amount;
    private LocalDate receivedAt;

    private Income(String id, String userId, String accountId, String description, BigDecimal amount,
                   LocalDate receivedAt, boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.accountId = accountId;
        this.description = description;
        this.amount = amount;
        this.receivedAt = receivedAt;
    }

    public static Income create(String id, String userId, String accountId, String description, BigDecimal amount,
                                LocalDate receivedAt, Instant now) {
        validateAmount(amount);
        return new Income(id, userId, accountId, description, amount, receivedAt, false, null, now, now);
    }

    public static Income rehydrate(String id, String userId, String accountId, String description, BigDecimal amount,
                                   LocalDate receivedAt, boolean deleted, Instant deletedAt, Instant createdAt,
                                   Instant updatedAt) {
        return new Income(id, userId, accountId, description, amount, receivedAt, deleted, deletedAt, createdAt, updatedAt);
    }

    public void changeAccount(String accountId) {
        this.accountId = accountId;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeAmount(BigDecimal amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    public void changeReceivedAt(LocalDate receivedAt) {
        this.receivedAt = receivedAt;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Valor deve ser maior que zero");
        }
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Income income)) {
            return false;
        }
        return id != null && id.equals(income.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
