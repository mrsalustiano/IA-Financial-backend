package com.finance.scheduler;

import com.finance.application.command.GenerateMonthlyClosingCommand;
import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.in.GenerateMonthlyClosingUseCase;
import com.finance.domain.ports.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class MonthlyClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyClosingScheduler.class);

    private final GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public MonthlyClosingScheduler(GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase,
                                   UserRepositoryPort userRepositoryPort) {
        this.generateMonthlyClosingUseCase = generateMonthlyClosingUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    /**
     * Executa o fechamento do mes anterior para cada usuario ativo; falha de um usuario nao
     * interrompe os demais (idempotente por usuario+mes).
     */
    @Scheduled(cron = "${app.monthly-closing.cron:0 5 0 1 * *}")
    public void closePreviousMonth() {
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        var userIds = userRepositoryPort.findAllActiveUserIds();
        log.info("Scheduled monthly closing starting month={} users={}", previousMonth, userIds.size());
        for (String userId : userIds) {
            try {
                MonthlyClosing closing = generateMonthlyClosingUseCase.handleForUser(userId, previousMonth);
                log.info("Scheduled monthly closing done userId={} month={} balance={}", userId, previousMonth,
                    closing.getBalance());
            } catch (RuntimeException ex) {
                log.error("Falha ao executar fechamento mensal agendado para userId={} mês={}", userId,
                    previousMonth, ex);
            }
        }
    }

    public MonthlyClosing run(GenerateMonthlyClosingCommand command) {
        return generateMonthlyClosingUseCase.handle(command.month());
    }
}
