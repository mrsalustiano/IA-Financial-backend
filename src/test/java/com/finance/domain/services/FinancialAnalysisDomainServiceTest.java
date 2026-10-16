package com.finance.domain.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAnalysisDomainServiceTest {

    private final FinancialAnalysisDomainService service = new FinancialAnalysisDomainService();

    @Test
    void scoreIsZeroWithoutData() {
        assertThat(service.calculateScore(0, 0, 0)).isZero();
    }

    @Test
    void scoreIsFullWithPlentyOfData() {
        assertThat(service.calculateScore(10, 5, 12)).isEqualTo(100);
    }

    @Test
    void scoreIsProportional() {
        assertThat(service.calculateScore(5, 3, 6)).isEqualTo(100);
        assertThat(service.calculateScore(1, 1, 1)).isEqualTo(23);
    }

    @Test
    void countsDistinctMonths() {
        long months = service.countMonthsCovered(List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
            LocalDate.of(2026, 10, 5)));
        assertThat(months).isEqualTo(2);
    }

    @Test
    void sumsAmounts() {
        assertThat(service.totalIncome(List.of(new BigDecimal("100.50"), new BigDecimal("200"))))
            .isEqualByComparingTo(new BigDecimal("300.50"));
    }

    @Test
    void buildsPromptWithSummaryAndFocus() {
        String prompt = service.buildPrompt(2, 5, new BigDecimal("1000"), 3, "economia");
        assertThat(prompt).contains("Contas ativas: 2");
        assertThat(prompt).contains("Receitas registradas: 5");
        assertThat(prompt).contains("Total de receitas: 1000");
        assertThat(prompt).contains("Meses com movimentacao: 3");
        assertThat(prompt).contains("Foco solicitado: economia");
    }

    @Test
    void parsesRecommendationLines() {
        List<String> recommendations = service.parseRecommendations(
            "Analise geral ok.\n- Cortar assinaturas\n* Aumentar reserva\n1. Revisar gastos\n\nFim.");
        assertThat(recommendations).containsExactly("Cortar assinaturas", "Aumentar reserva", "Revisar gastos");
    }
}
