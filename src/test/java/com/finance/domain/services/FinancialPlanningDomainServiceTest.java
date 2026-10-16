package com.finance.domain.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPlanningDomainServiceTest {

    private final FinancialPlanningDomainService service = new FinancialPlanningDomainService();

    @Test
    void buildsPlanPromptWithGoalAndSummary() {
        String prompt = service.buildPlanPrompt("juntar 10 mil", new BigDecimal("10000"),
            LocalDate.of(2026, 12, 31), 2, 5, new BigDecimal("8000"), 3);
        assertThat(prompt).contains("Meta do usuario: juntar 10 mil");
        assertThat(prompt).contains("Valor alvo: 10000");
        assertThat(prompt).contains("Data alvo: 2026-12-31");
        assertThat(prompt).contains("Contas ativas: 2");
        assertThat(prompt).contains("Total de receitas: 8000");
    }

    @Test
    void buildsPlanPromptWithMissingTarget() {
        String prompt = service.buildPlanPrompt("viajar", null, null, 1, 1, BigDecimal.TEN, 1);
        assertThat(prompt).contains("Valor alvo: nao informado");
        assertThat(prompt).contains("Data alvo: nao informada");
    }

    @Test
    void buildsAssistantPromptWithQuestionAndContext() {
        String prompt = service.buildAssistantPrompt("qual meu saldo?", 2, 4, new BigDecimal("500"), 2);
        assertThat(prompt).contains("Pergunta do usuario: qual meu saldo?");
        assertThat(prompt).contains("Receitas registradas: 4");
        assertThat(prompt).contains("Meses com movimentacao: 2");
    }

    @Test
    void parsesPlanStepsOnlyFromBullets() {
        List<String> steps = service.parsePlanSteps(
            "Plano viavel.\n- Cortar delivery\n* Guardar 10% \n1. Revisar assinaturas");
        assertThat(steps).containsExactly("Cortar delivery", "Guardar 10%", "Revisar assinaturas");
    }
}
