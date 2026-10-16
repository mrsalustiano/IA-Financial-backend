package com.finance.scheduler;

import com.finance.application.command.GenerateMonthlyClosingCommand;
import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.GenerateMonthlyClosingUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class MonthlyClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyClosingScheduler.class);

    private final GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase;

    public MonthlyClosingScheduler(GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase) {
        this.generateMonthlyClosingUseCase = generateMonthlyClosingUseCase;
    }

    @Scheduled(cron = "${app.monthly-closing.cron:0 5 0 1 * *}")
    public void closePreviousMonth() {
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        try {
            MonthlyClosing closing = generateMonthlyClosingUseCase.handle(previousMonth);
            log.info("Scheduled monthly closing done month={} balance={}", previousMonth, closing.getBalance());
        } catch (RuntimeException ex) {
            log.error("Falha ao executar fechamento mensal agendado para {}", previousMonth, ex);
        }
    }

    public MonthlyClosing run(GenerateMonthlyClosingCommand command) {
        return generateMonthlyClosingUseCase.handle(command.month());
    }
}
