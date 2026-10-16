package com.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class MonthlyClosing extends BaseEntity {

    private String referenceMonth;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private Instant closedAt;

    private MonthlyClosing(String id, String userId, String referenceMonth, BigDecimal totalIncome,
                           BigDecimal totalExpense, BigDecimal balance, Instant closedAt, boolean deleted,
                           Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.referenceMonth = referenceMonth;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = balance;
        this.closedAt = closedAt;
    }

    public static MonthlyClosing create(String id, String userId, String referenceMonth, BigDecimal totalIncome,
                                        BigDecimal totalExpense, Instant closedAt, Instant now) {
        BigDecimal balance = totalIncome.subtract(totalExpense);
        return new MonthlyClosing(id, userId, referenceMonth, totalIncome, totalExpense, balance, closedAt, false,
            null, now, now);
    }

    public static MonthlyClosing rehydrate(String id, String userId, String referenceMonth, BigDecimal totalIncome,
                                           BigDecimal totalExpense, BigDecimal balance, Instant closedAt,
                                           boolean deleted, Instant deletedAt, Instant createdAt,
                                           Instant updatedAt) {
        return new MonthlyClosing(id, userId, referenceMonth, totalIncome, totalExpense, balance, closedAt, deleted,
            deletedAt, createdAt, updatedAt);
    }

    public void updateTotals(BigDecimal totalIncome, BigDecimal totalExpense, Instant closedAt, Instant now) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = totalIncome.subtract(totalExpense);
        this.closedAt = closedAt;
        touch(now);
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MonthlyClosing closing)) {
            return false;
        }
        return id != null && id.equals(closing.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
