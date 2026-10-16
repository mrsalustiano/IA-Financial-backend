package com.finance.application.usecase;

import com.finance.application.command.GenerateFinancialAnalysisCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.FinancialAnalysisGeneratedEvent;
import com.finance.domain.model.AiPrompt;
import com.finance.domain.model.AiResponse;
import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.ports.in.GenerateFinancialAnalysisUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.FinancialAnalysisRepositoryPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.ValidationException;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import com.finance.domain.ports.out.CurrentUserPort;

@Service
public class GenerateFinancialAnalysisUseCaseImpl implements GenerateFinancialAnalysisUseCase {

    private final CurrentUserPort currentUserPort;

    private static final Logger log = LoggerFactory.getLogger(GenerateFinancialAnalysisUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final IncomeRepositoryPort incomeRepositoryPort;
    private final FinancialAnalysisRepositoryPort analysisRepositoryPort;
    private final AiProviderPort aiProviderPort;
    private final FinancialAnalysisDomainService analysisDomainService;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public GenerateFinancialAnalysisUseCaseImpl(AccountRepositoryPort accountRepositoryPort,
                                                IncomeRepositoryPort incomeRepositoryPort,
                                                FinancialAnalysisRepositoryPort analysisRepositoryPort,
                                                AiProviderPort aiProviderPort,
                                                FinancialAnalysisDomainService analysisDomainService,
                                                AuditPort auditPort, EventPublisherPort eventPublisherPort,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.analysisRepositoryPort = analysisRepositoryPort;
        this.aiProviderPort = aiProviderPort;
        this.analysisDomainService = analysisDomainService;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public FinancialAnalysis handle(GenerateFinancialAnalysisCommand command) {
        log.info("Generating financial analysis");
        Instant now = Instant.now();
        int accountCount = accountRepositoryPort
            .search(new AccountSearchQuery(null, true, PageRequest.of(0, 500, Sort.unsorted())))
            .content().size();
        PageResult<com.finance.domain.model.Income> incomes = incomeRepositoryPort
            .search(new IncomeSearchQuery(currentUserPort.requireUserId(), null, null, null,
                PageRequest.of(0, 500, Sort.unsorted())));

        if (incomes.content().isEmpty()) {
            throw new ValidationException("Não há receitas suficientes para gerar uma análise");
        }

        String prompt = analysisDomainService.buildPrompt(accountCount, incomes.content().size(),
            analysisDomainService.totalIncome(incomes.content().stream()
                .map(com.finance.domain.model.Income::getAmount).toList()),
            analysisDomainService.countMonthsCovered(incomes.content().stream()
                .map(com.finance.domain.model.Income::getReceivedAt).toList()),
            command.focus());

        AiResponse aiResponse = aiProviderPort.complete(new AiPrompt(prompt, java.util.Map.of()));
        List<String> recommendations = analysisDomainService.parseRecommendations(aiResponse.content());
        long score = analysisDomainService.calculateScore(incomes.content().size(), accountCount,
            analysisDomainService.countMonthsCovered(incomes.content().stream()
                .map(com.finance.domain.model.Income::getReceivedAt).toList()));

        FinancialAnalysis analysis = FinancialAnalysis.create(Identifiers.newId(), currentUserPort.requireUserId(),
            prompt, aiResponse.content(), score, recommendations, aiResponse.provider(), aiResponse.model(), now);
        FinancialAnalysis saved = analysisRepositoryPort.save(analysis);
        auditPort.record(AppConstants.ENTITY_FINANCIAL_ANALYSIS, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new FinancialAnalysisGeneratedEvent(saved.getId(), saved.getUserId(),
            saved.getScore(), now));
        log.info("Financial analysis generated id={} score={}", saved.getId(), saved.getScore());
        return saved;
    }
}
