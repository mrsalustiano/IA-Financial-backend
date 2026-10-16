package com.finance.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateIncomeCommand(String accountId, String description, BigDecimal amount, LocalDate receivedAt) {
}
