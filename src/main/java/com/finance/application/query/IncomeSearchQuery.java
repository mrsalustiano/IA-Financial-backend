package com.finance.application.query;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * userId preenchido apenas por caminhos sem contexto de requisicao (scheduler);
 * em requisicoes autenticadas o adapter resolve pelo token.
 */
public record IncomeSearchQuery(String userId, String accountId, LocalDate startDate, LocalDate endDate,
                                Pageable pageable) {
}
