package com.finance.adapters.out.mongodb;

import com.finance.domain.model.Income;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IncomePersistenceMapperTest {

    private final IncomePersistenceMapper mapper = Mappers.getMapper(IncomePersistenceMapper.class);

    @Test
    void mapsDomainToDocument() {
        Instant now = Instant.now();
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("1234.56"),
            LocalDate.of(2026, 10, 5), now);

        IncomeDocument document = mapper.toDocument(income);

        assertThat(document.getId()).isEqualTo("inc-1");
        assertThat(document.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(document.getAccountId()).isEqualTo("acc-1");
        assertThat(document.getDescription()).isEqualTo("Salary");
        assertThat(document.getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(document.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(document.isDeleted()).isFalse();
        assertThat(document.getDeletedAt()).isNull();
    }

    @Test
    void mapsDocumentToDomain() {
        Instant now = Instant.now();
        IncomeDocument document = new IncomeDocument("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("10"),
            LocalDate.of(2026, 10, 5), true, now, now, now);

        Income income = mapper.toDomain(document);

        assertThat(income.getId()).isEqualTo("inc-1");
        assertThat(income.getAmount()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(income.getReceivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(income.isDeleted()).isTrue();
        assertThat(income.getDeletedAt()).isEqualTo(now);
    }
}
