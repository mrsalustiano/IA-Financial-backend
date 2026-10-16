package com.finance.application.command;

import com.finance.domain.enums.AccountType;

public record UpdateAccountCommand(String name, String description, AccountType type, Boolean active) {
}
