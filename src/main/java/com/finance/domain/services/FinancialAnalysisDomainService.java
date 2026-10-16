package com.finance.domain.services;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Regras deterministicas de score financeiro (0-100):
 * ate 40 pontos por receitas registradas, 30 por contas ativas e 30 por meses cobertos.
 */
@Component
public class FinancialAnalysisDomainService {

    public long calculateScore(int incomeCount, int accountCount, long monthsCovered) {
        long incomePoints = Math.round(40.0 * Math.min(incomeCount, 5) / 5.0);
        long accountPoints = Math.round(30.0 * Math.min(accountCount, 3) / 3.0);
        long monthPoints = Math.round(30.0 * Math.min(monthsCovered, 6) / 6.0);
        return incomePoints + accountPoints + monthPoints;
    }

    public long countMonthsCovered(List<LocalDate> receivedDates) {
        return receivedDates.stream().map(YearMonth::from).distinct().count();
    }

    public BigDecimal totalIncome(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String buildPrompt(int accountCount, int incomeCount, BigDecimal totalIncome, long monthsCovered,
                              String focus) {
        return """
            Voce e um analista financeiro pessoal. Analise os dados abaixo e retorne:
            1. Uma avaliacao curta da saude financeira.
            2. Uma lista de recomendacoes praticas, uma por linha, iniciadas por "-".

            Resumo financeiro do usuario:
            - Contas ativas: %d
            - Receitas registradas: %d
            - Total de receitas: %s
            - Meses com movimentacao: %d
            %s
            """.formatted(accountCount, incomeCount, totalIncome.toPlainString(), monthsCovered,
            focus == null || focus.isBlank() ? "" : "- Foco solicitado: " + focus);
    }

    public List<String> parseRecommendations(String aiResponse) {
        return aiResponse.lines()
            .map(String::trim)
            .filter(line -> line.matches("^(-|\\*|\\d+[.)])\\s+.*"))
            .map(line -> line.replaceFirst("^(-|\\*|\\d+[.)])\\s*", ""))
            .filter(line -> !line.isBlank())
            .limit(10)
            .toList();
    }
}
