package com.finance.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFinancialPlanCommand(String goal, BigDecimal targetAmount, LocalDate targetDate) {
}
