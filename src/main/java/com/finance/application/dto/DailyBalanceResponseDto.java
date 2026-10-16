package com.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBalanceResponseDto(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense,
                                      BigDecimal balance, BigDecimal accumulated) {
}
