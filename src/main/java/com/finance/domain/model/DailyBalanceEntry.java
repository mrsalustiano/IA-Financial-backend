package com.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBalanceEntry(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense,
                                BigDecimal accumulated) {

    public DailyBalanceEntry {
        BigDecimal income = totalIncome == null ? BigDecimal.ZERO : totalIncome;
        BigDecimal expense = totalExpense == null ? BigDecimal.ZERO : totalExpense;
        totalIncome = income;
        totalExpense = expense;
        accumulated = accumulated == null ? income.subtract(expense) : accumulated;
    }

    public DailyBalanceEntry(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense) {
        this(date, totalIncome, totalExpense, null);
    }

    public BigDecimal balance() {
        return totalIncome.subtract(totalExpense);
    }
}
