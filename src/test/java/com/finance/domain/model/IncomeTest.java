package com.finance.domain.model;

import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncomeTest {

    private static final Instant NOW = Instant.now();

    @Test
    void createValidIncome() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100.10"),
            LocalDate.of(2026, 10, 5), NOW);

        assertThat(income.getId()).isEqualTo("inc-1");
        assertThat(income.getAccountId()).isEqualTo("acc-1");
        assertThat(income.getAmount()).isEqualByComparingTo(new BigDecimal("100.10"));
        assertThat(income.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(income.isDeleted()).isFalse();
    }

    @Test
    void createRejectsZeroAmount() {
        assertThatThrownBy(() -> Income.create("inc-1", "LOCAL_USER", "acc-1", "Zero", BigDecimal.ZERO,
            LocalDate.of(2026, 10, 5), NOW))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("maior que zero");
    }

    @Test
    void createRejectsNegativeAmount() {
        assertThatThrownBy(() -> Income.create("inc-1", "LOCAL_USER", "acc-1", "Negative",
            new BigDecimal("-1"), LocalDate.of(2026, 10, 5), NOW))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsNullAmount() {
        assertThatThrownBy(() -> Income.create("inc-1", "LOCAL_USER", "acc-1", "Null", null,
            LocalDate.of(2026, 10, 5), NOW))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void changeAmountValidatesPositiveValue() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);

        income.changeAmount(new BigDecimal("150"));
        assertThat(income.getAmount()).isEqualByComparingTo(new BigDecimal("150"));

        assertThatThrownBy(() -> income.changeAmount(new BigDecimal("0")))
            .isInstanceOf(ValidationException.class);
        assertThat(income.getAmount()).isEqualByComparingTo(new BigDecimal("150"));
    }

    @Test
    void changeAccountAndDescriptionAndDate() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);

        income.changeAccount("acc-2");
        income.changeDescription("Bonus");
        income.changeReceivedAt(LocalDate.of(2026, 10, 6));
        income.touch(NOW.plusSeconds(10));

        assertThat(income.getAccountId()).isEqualTo("acc-2");
        assertThat(income.getDescription()).isEqualTo("Bonus");
        assertThat(income.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(income.getUpdatedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void markDeletedAndRehydrate() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);

        income.markDeleted(NOW.plusSeconds(30));
        assertThat(income.isDeleted()).isTrue();

        Income restored = Income.rehydrate("inc-2", "LOCAL_USER", "acc-1", "Restored", new BigDecimal("50"),
            LocalDate.of(2026, 10, 5), true, NOW, NOW, NOW);
        assertThat(restored.getId()).isEqualTo("inc-2");
        assertThat(restored.isDeleted()).isTrue();
    }

    @Test
    void equalsIsIdBased() {
        Income a = Income.create("inc-1", "LOCAL_USER", "acc-1", "A", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);
        Income b = Income.create("inc-1", "LOCAL_USER", "acc-1", "B", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);
        Income c = Income.create("inc-2", "LOCAL_USER", "acc-1", "C", new BigDecimal("100"),
            LocalDate.of(2026, 10, 5), NOW);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new Object());
    }
}
