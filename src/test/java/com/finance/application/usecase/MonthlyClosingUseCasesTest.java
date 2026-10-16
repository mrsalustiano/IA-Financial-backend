package com.finance.application.usecase;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.MonthlyClosingGeneratedEvent;
import com.finance.domain.model.Income;
import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.scheduler.MonthlyClosingScheduler;
import com.finance.shared.exception.NotFoundException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyClosingUseCasesTest {

    @Mock
    private IncomeRepositoryPort incomeRepositoryPort;

    @Mock
    private MonthlyClosingRepositoryPort closingRepositoryPort;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private GenerateMonthlyClosingUseCaseImpl generateUseCase;

    @Test
    void rejectsInvalidMonthFormat() {
        assertThatThrownBy(() -> generateUseCase.handleForUser("LOCAL_USER", "2026-13"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("Mês inválido");
        assertThatThrownBy(() -> generateUseCase.handleForUser("LOCAL_USER", "mes passado"))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> generateUseCase.handleForUser("LOCAL_USER", null))
            .isInstanceOf(ValidationException.class);
        verify(closingRepositoryPort, never()).save(any());
    }

    @Test
    void createsClosingSnapshotWithAuditAndEvent() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salario", new BigDecimal("2000"),
            LocalDate.of(2026, 9, 10), Instant.now());
        when(currentUserPort.requireUserId()).thenReturn("LOCAL_USER");
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(income), 0, 500, 1, 1));
        when(closingRepositoryPort.findByUserAndReferenceMonth("LOCAL_USER", "2026-09"))
            .thenReturn(Optional.empty());
        when(closingRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyClosing closing = generateUseCase.handle("2026-09");

        assertThat(closing.getReferenceMonth()).isEqualTo("2026-09");
        assertThat(closing.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(closing.getTotalIncome()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(closing.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(closing.getBalance()).isEqualByComparingTo(new BigDecimal("2000"));
        verify(auditPort).record(eq("MonthlyClosing"), eq(closing.getId()), eq(AuditAction.CRIACAO), isNull(),
            eq(closing));
        ArgumentCaptor<MonthlyClosingGeneratedEvent> eventCaptor =
            ArgumentCaptor.forClass(MonthlyClosingGeneratedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().referenceMonth()).isEqualTo("2026-09");
        assertThat(eventCaptor.getValue().balance()).isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    void updatesExistingSnapshotWithAudit() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salario", new BigDecimal("3000"),
            LocalDate.of(2026, 9, 10), Instant.now());
        when(currentUserPort.requireUserId()).thenReturn("LOCAL_USER");
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(income), 0, 500, 1, 1));
        MonthlyClosing existing = MonthlyClosing.create("mc-1", "LOCAL_USER", "2026-09", new BigDecimal("1000"),
            BigDecimal.ZERO, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));
        when(closingRepositoryPort.findByUserAndReferenceMonth("LOCAL_USER", "2026-09"))
            .thenReturn(Optional.of(existing));
        when(closingRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        MonthlyClosing closing = generateUseCase.handle("2026-09");

        assertThat(closing.getId()).isEqualTo("mc-1");
        assertThat(closing.getTotalIncome()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(closing.getUpdatedAt()).isAfter(Instant.parse("2026-10-01T00:00:00Z"));
        verify(auditPort).record(eq("MonthlyClosing"), eq("mc-1"), eq(AuditAction.ATUALIZACAO), eq("snapshot"),
            eq(closing));
    }

    @Test
    void sumsAllIncomePages() {
        List<Income> page1 = List.of(Income.create("i1", "LOCAL_USER", "a", "r1", new BigDecimal("100"),
            LocalDate.of(2026, 9, 1), Instant.now()));
        List<Income> page2 = List.of(Income.create("i2", "LOCAL_USER", "a", "r2", new BigDecimal("200"),
            LocalDate.of(2026, 9, 20), Instant.now()));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class))).thenReturn(
            new PageResult<>(page1, 0, 1, 2, 2),
            new PageResult<>(page2, 1, 1, 2, 2));
        when(closingRepositoryPort.findByUserAndReferenceMonth("LOCAL_USER", "2026-09"))
            .thenReturn(Optional.empty());
        when(closingRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyClosing closing = generateUseCase.handleForUser("LOCAL_USER", "2026-09");

        assertThat(closing.getTotalIncome()).isEqualByComparingTo(new BigDecimal("300"));
        verify(incomeRepositoryPort, org.mockito.Mockito.times(2)).search(any(IncomeSearchQuery.class));
    }

    @Test
    void getsAndListsClosings() {
        GetMonthlyClosingByIdUseCaseImpl getUseCase = new GetMonthlyClosingByIdUseCaseImpl(closingRepositoryPort,
            currentUserPort);
        ListMonthlyClosingsUseCaseImpl listUseCase = new ListMonthlyClosingsUseCaseImpl(closingRepositoryPort,
            currentUserPort);
        MonthlyClosing closing = MonthlyClosing.create("mc-1", "LOCAL_USER", "2026-09", BigDecimal.TEN, BigDecimal.ZERO,
            Instant.now(), Instant.now());
        when(currentUserPort.requireUserId()).thenReturn("LOCAL_USER");
        when(closingRepositoryPort.findByIdAndUser("LOCAL_USER", "mc-1")).thenReturn(Optional.of(closing));
        assertThat(getUseCase.handle("mc-1")).isSameAs(closing);
        when(closingRepositoryPort.findByIdAndUser("LOCAL_USER", "missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> getUseCase.handle("missing")).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Fechamento mensal não encontrado");
        when(closingRepositoryPort.findAllByUser("LOCAL_USER")).thenReturn(List.of(closing));
        assertThat(listUseCase.handle()).containsExactly(closing);
    }

    @Test
    void schedulerClosesPreviousMonthForEachActiveUser() {
        when(userRepositoryPort.findAllActiveUserIds()).thenReturn(List.of("user-1", "user-2"));
        when(closingRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(incomeRepositoryPort.search(any(IncomeSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 500, 0, 0));
        MonthlyClosingScheduler scheduler = new MonthlyClosingScheduler(generateUseCase, userRepositoryPort);

        scheduler.closePreviousMonth();

        verify(closingRepositoryPort).findByUserAndReferenceMonth("user-1", YearMonth.now().minusMonths(1).toString());
        verify(closingRepositoryPort).findByUserAndReferenceMonth("user-2", YearMonth.now().minusMonths(1).toString());
    }

    @Test
    void schedulerDelegatesAndSwallowsFailures() {
        when(userRepositoryPort.findAllActiveUserIds()).thenReturn(List.of("user-1"));
        MonthlyClosingScheduler scheduler = new MonthlyClosingScheduler(new com.finance.domain.ports.in
            .GenerateMonthlyClosingUseCase() {
            @Override
            public MonthlyClosing handle(String referenceMonth) {
                throw new IllegalStateException("boom");
            }

            @Override
            public MonthlyClosing handleForUser(String userId, String referenceMonth) {
                throw new IllegalStateException("boom");
            }
        }, userRepositoryPort);
        assertThatCode(scheduler::closePreviousMonth).doesNotThrowAnyException();
    }
}
