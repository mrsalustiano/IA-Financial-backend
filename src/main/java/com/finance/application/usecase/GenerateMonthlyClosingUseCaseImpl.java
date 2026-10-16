package com.finance.application.usecase;

import com.finance.application.command.GenerateMonthlyClosingCommand;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.MonthlyClosingGeneratedEvent;
import com.finance.domain.model.Income;
import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.GenerateMonthlyClosingUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.ValidationException;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
public class GenerateMonthlyClosingUseCaseImpl implements GenerateMonthlyClosingUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateMonthlyClosingUseCaseImpl.class);
    private static final java.util.regex.Pattern MONTH_PATTERN = java.util.regex.Pattern
        .compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final IncomeRepositoryPort incomeRepositoryPort;
    private final MonthlyClosingRepositoryPort closingRepositoryPort;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public GenerateMonthlyClosingUseCaseImpl(IncomeRepositoryPort incomeRepositoryPort,
                                             MonthlyClosingRepositoryPort closingRepositoryPort, AuditPort auditPort,
                                             EventPublisherPort eventPublisherPort) {
        this.incomeRepositoryPort = incomeRepositoryPort;
        this.closingRepositoryPort = closingRepositoryPort;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public MonthlyClosing handle(String referenceMonth) {
        if (referenceMonth == null || !MONTH_PATTERN.matcher(referenceMonth).matches()) {
            throw new ValidationException("Mês inválido. Use o formato yyyy-MM");
        }
        log.info("Generating monthly closing for {}", referenceMonth);
        YearMonth month = YearMonth.parse(referenceMonth);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        BigDecimal totalIncome = sumIncomes(startDate, endDate);
        BigDecimal totalExpense = BigDecimal.ZERO;
        Instant now = Instant.now();

        MonthlyClosing existing = closingRepositoryPort.findByReferenceMonth(referenceMonth).orElse(null);
        MonthlyClosing closing;
        if (existing == null) {
            closing = MonthlyClosing.create(Identifiers.newId(), AppConstants.LOCAL_USER_ID, referenceMonth,
                totalIncome, totalExpense, now, now);
            closing = closingRepositoryPort.save(closing);
            auditPort.record(AppConstants.ENTITY_MONTHLY_CLOSING, closing.getId(), AuditAction.CRIACAO, null,
                closing);
        } else {
            Object oldValue = auditPort.snapshot(existing);
            existing.updateTotals(totalIncome, totalExpense, now, now);
            closing = closingRepositoryPort.save(existing);
            auditPort.record(AppConstants.ENTITY_MONTHLY_CLOSING, closing.getId(), AuditAction.ATUALIZACAO,
                oldValue, closing);
        }
        eventPublisherPort.publish(new MonthlyClosingGeneratedEvent(closing.getId(), closing.getUserId(),
            referenceMonth, closing.getBalance(), now));
        log.info("Monthly closing generated month={} balance={}", referenceMonth, closing.getBalance());
        return closing;
    }

    private BigDecimal sumIncomes(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = BigDecimal.ZERO;
        int page = 0;
        long consumed = 0;
        long totalElements = 0;
        do {
            PageResult<Income> result = incomeRepositoryPort.search(new IncomeSearchQuery(null, startDate, endDate,
                PageRequest.of(page, 500, Sort.unsorted())));
            total = total.add(result.content().stream().map(Income::getAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add));
            consumed += result.content().size();
            totalElements = result.totalElements();
            page++;
        } while (consumed < totalElements);
        return total;
    }
}
