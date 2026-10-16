package com.finance.domain.services;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Monta prompts deterministicos de planejamento e assistência usando o contexto financeiro.
 */
@Component
public class FinancialPlanningDomainService {

    public String buildPlanPrompt(String goal, BigDecimal targetAmount, LocalDate targetDate, int accountCount,
                                  int incomeCount, BigDecimal totalIncome, long monthsCovered) {
        return """
            Voce e um planejador financeiro pessoal. Com base nos dados abaixo, crie um plano de economia
            para a meta informada. Retorne:
            1. Uma avaliacao curta da viabilidade da meta.
            2. Uma lista de passos praticos, um por linha, iniciados por "-".

            Meta do usuario: %s
            Valor alvo: %s
            Data alvo: %s

            Resumo financeiro do usuario:
            - Contas ativas: %d
            - Receitas registradas: %d
            - Total de receitas: %s
            - Meses com movimentacao: %d
            """.formatted(goal, targetAmount == null ? "nao informado" : targetAmount.toPlainString(),
            targetDate == null ? "nao informada" : targetDate.toString(), accountCount, incomeCount,
            totalIncome.toPlainString(), monthsCovered);
    }

    public String buildAssistantPrompt(String question, int accountCount, int incomeCount, BigDecimal totalIncome,
                                       long monthsCovered) {
        return """
            Voce e o assistente financeiro pessoal do usuario. Responda em portugues, de forma direta,
            usando apenas os dados abaixo. Se a resposta nao estiver nos dados, diga que nao ha
            informacao suficiente.

            Pergunta do usuario: %s

            Contexto financeiro do usuario:
            - Contas ativas: %d
            - Receitas registradas: %d
            - Total de receitas: %s
            - Meses com movimentacao: %d
            """.formatted(question, accountCount, incomeCount, totalIncome.toPlainString(), monthsCovered);
    }

    public java.util.List<String> parsePlanSteps(String aiResponse) {
        return aiResponse.lines()
            .map(String::trim)
            .filter(line -> line.matches("^(-|\\*|\\d+[.)])\\s+.*"))
            .map(line -> line.replaceFirst("^(-|\\*|\\d+[.)])\\s*", ""))
            .filter(line -> !line.isBlank())
            .limit(10)
            .toList();
    }
}
