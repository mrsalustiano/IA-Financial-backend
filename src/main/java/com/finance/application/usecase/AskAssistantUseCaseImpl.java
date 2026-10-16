package com.finance.application.usecase;

import com.finance.application.command.AskAssistantCommand;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.AssistantMessageCreatedEvent;
import com.finance.domain.model.AssistantMessage;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.AskAssistantUseCase;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.domain.ports.out.AssistantMessageRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.services.FinancialAnalysisDomainService;
import com.finance.domain.services.FinancialPlanningDomainService;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AskAssistantUseCaseImpl implements AskAssistantUseCase {

    private static final Logger log = LoggerFactory.getLogger(AskAssistantUseCaseImpl.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final IncomeRepositoryPort incomeRepositoryPort;
    private final AssistantMessageRepositoryPort messageRepositoryPort;
    private final AiProviderPort aiProviderPort;
    private final FinancialAnalysisDomainService analysisDomainService;
    private final FinancialPlanningDomainService planningDomainService;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public AskAssistantUseCaseImpl(AccountRepositoryPort accountRepositoryPort,
                                   IncomeRepositoryPort incomeRepositoryPort,
                                   AssistantMessageRepositoryPort messageRepositoryPort,
                                   AiProviderPort aiProviderPort,
                                   FinancialAnalysisDomainService analysisDomainService,
                                   FinancialPlanningDomainService planningDomainService, AuditPort auditPort,
                                   EventPublisherPort eventPublisherPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.messageRepositoryPort = messageRepositoryPort;
        this.aiProviderPort = aiProviderPort;
        this.analysisDomainService = analysisDomainService;
        this.planningDomainService = planningDomainService;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public AssistantMessage handle(AskAssistantCommand command) {
        log.info("Assistant question received");
        Instant now = Instant.now();
        int accountCount = accountRepositoryPort
            .search(new AccountSearchQuery(null, true, PageRequest.of(0, 500, Sort.unsorted())))
            .content().size();
        PageResult<Income> incomes = incomeRepositoryPort
            .search(new IncomeSearchQuery(null, null, null, PageRequest.of(0, 500, Sort.unsorted())));
        long monthsCovered = analysisDomainService.countMonthsCovered(incomes.content().stream()
            .map(Income::getReceivedAt).toList());
        java.math.BigDecimal totalIncome = analysisDomainService.totalIncome(incomes.content().stream()
            .map(Income::getAmount).toList());
        String prompt = planningDomainService.buildAssistantPrompt(command.question(), accountCount,
            incomes.content().size(), totalIncome, monthsCovered);
        com.finance.domain.model.AiResponse aiResponse = aiProviderPort
            .complete(new com.finance.domain.model.AiPrompt(prompt, java.util.Map.of()));
        AssistantMessage message = AssistantMessage.create(Identifiers.newId(), AppConstants.LOCAL_USER_ID,
            command.question(), aiResponse.content(), aiResponse.provider(), aiResponse.model(), now);
        AssistantMessage saved = messageRepositoryPort.save(message);
        auditPort.record(AppConstants.ENTITY_ASSISTANT_MESSAGE, saved.getId(), AuditAction.CRIACAO, null, saved);
        eventPublisherPort.publish(new AssistantMessageCreatedEvent(saved.getId(), saved.getUserId(), now));
        log.info("Assistant message saved id={}", saved.getId());
        return saved;
    }
}
