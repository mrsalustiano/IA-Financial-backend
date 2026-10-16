package com.finance.adapters.out.mongodb;

import com.finance.domain.model.MonthlyClosing;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyClosingPersistenceMapperTest {

    private final MonthlyClosingPersistenceMapper mapper = Mappers.getMapper(MonthlyClosingPersistenceMapper.class);

    @Test
    void mapsBothWays() {
        Instant now = Instant.now();
        MonthlyClosing closing = MonthlyClosing.create("mc-1", "LOCAL_USER", "2026-09", new BigDecimal("3000"),
            BigDecimal.ZERO, now, now);
        MonthlyClosingDocument document = mapper.toDocument(closing);
        assertThat(document.getReferenceMonth()).isEqualTo("2026-09");
        assertThat(document.getTotalIncome()).isEqualByComparingTo(new BigDecimal("3000"));
        MonthlyClosing domain = mapper.toDomain(document);
        assertThat(domain.getBalance()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(domain.getCreatedAt()).isEqualTo(document.getCreatedAt());
    }
}
