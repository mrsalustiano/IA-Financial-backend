package com.finance.adapters.out.mongodb;

import com.finance.domain.model.FinancialPlan;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPlanPersistenceMapperTest {

    private final FinancialPlanPersistenceMapper mapper = Mappers.getMapper(FinancialPlanPersistenceMapper.class);

    @Test
    void mapsBothWays() {
        Instant now = Instant.now();
        FinancialPlan plan = FinancialPlan.create("fp-1", "LOCAL_USER", "meta", BigDecimal.TEN,
            LocalDate.of(2026, 12, 31), "p", "r", List.of("s1", "s2"), "ollama", "llama3", now);
        FinancialPlanDocument document = mapper.toDocument(plan);
        assertThat(document.getGoal()).isEqualTo("meta");
        assertThat(document.getRecommendations()).containsExactly("s1", "s2");
        FinancialPlan domain = mapper.toDomain(document);
        assertThat(domain.getTargetAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(domain.getRecommendations()).containsExactly("s1", "s2");
    }
}
