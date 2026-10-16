package com.finance.application.usecase;

import com.finance.application.command.CreateFinancialPlanCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.FinancialPlanCreatedEvent;
import com.finance.domain.model.FinancialPlan;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.CreateFinancialPlanUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.FinancialPlanRepositoryPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.domain.services.FinancialPlanningDomainService;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.ValidationException;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateFinancialPlanUseCaseImpl implements CreateFinancialPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateFinancialPlanUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final IncomeRepositoryPort incomeRepositoryPort;
    private final FinancialPlanRepositoryPort planRepositoryPort;
    private final AiProviderPort aiProviderPort;
    private final FinancialAnalysisDomainService analysisDomainService;
    private final FinancialPlanningDomainService planningDomainService;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateFinancialPlanUseCaseImpl(AccountRepositoryPort accountRepositoryPort,
                                          IncomeRepositoryPort incomeRepositoryPort,
                                          FinancialPlanRepositoryPort planRepositoryPort,
                                          AiProviderPort aiProviderPort,
                                          FinancialAnalysisDomainService analysisDomainService,
                                          FinancialPlanningDomainService planningDomainService, AuditPort auditPort,
                                          EventPublisherPort eventPublisherPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.planRepositoryPort = planRepositoryPort;
        this.aiProviderPort = aiProviderPort;
        this.analysisDomainService = analysisDomainService;
        this.planningDomainService = planningDomainService;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public FinancialPlan handle(CreateFinancialPlanCommand command) {
        log.info("Creating financial plan for goal={}", command.goal());
        Instant now = Instant.now();
        int accountCount = accountRepositoryPort
            .search(new AccountSearchQuery(null, true, PageRequest.of(0, 500, Sort.unsorted())))
            .content().size();
        PageResult<Income> incomes = incomeRepositoryPort
            .search(new IncomeSearchQuery(null, null, null, PageRequest.of(0, 500, Sort.unsorted())));
        if (incomes.content().isEmpty()) {
            throw new ValidationException("Não há receitas suficientes para criar um plano");
        }
        long monthsCovered = analysisDomainService.countMonthsCovered(incomes.content().stream()
            .map(Income::getReceivedAt).toList());
        java.math.BigDecimal totalIncome = analysisDomainService.totalIncome(incomes.content().stream()
            .map(Income::getAmount).toList());
        String prompt = planningDomainService.buildPlanPrompt(command.goal(), command.targetAmount(),
            command.targetDate(), accountCount, incomes.content().size(), totalIncome, monthsCovered);

        com.finance.domain.model.AiResponse aiResponse = aiProviderPort
            .complete(new com.finance.domain.model.AiPrompt(prompt, java.util.Map.of()));
        FinancialPlan plan = FinancialPlan.create(Identifiers.newId(), AppConstants.LOCAL_USER_ID, command.goal(),
            command.targetAmount(), command.targetDate(), prompt, aiResponse.content(),
            planningDomainService.parsePlanSteps(aiResponse.content()), aiResponse.provider(), aiResponse.model(),
            now);
        FinancialPlan saved = planRepositoryPort.save(plan);
        auditPort.record(AppConstants.ENTITY_FINANCIAL_PLAN, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new FinancialPlanCreatedEvent(saved.getId(), saved.getUserId(), now));
        log.info("Financial plan created id={}", saved.getId());
        return saved;
    }
}
