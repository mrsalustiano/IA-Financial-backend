package com.finance.application.mapper;

import com.finance.application.command.CreateFinancialPlanCommand;
import com.finance.application.dto.CreateFinancialPlanDto;
import com.finance.domain.model.FinancialPlan;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPlanDtoMapperTest {

    private final FinancialPlanDtoMapper mapper = Mappers.getMapper(FinancialPlanDtoMapper.class);

    @Test
    void mapsCreateDtoToCommand() {
        CreateFinancialPlanCommand command = mapper.toCommand(new CreateFinancialPlanDto("juntar 10 mil",
            new BigDecimal("10000"), LocalDate.of(2026, 12, 31)));
        assertThat(command.goal()).isEqualTo("juntar 10 mil");
        assertThat(command.targetAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(command.targetDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void mapsDomainToResponseDto() {
        FinancialPlan plan = FinancialPlan.create("fp-1", "LOCAL_USER", "meta", BigDecimal.TEN,
            LocalDate.of(2026, 12, 31), "prompt", "resposta", List.of("passo 1"), "ollama", "llama3", Instant.now());
        var dto = mapper.toResponseDto(plan);
        assertThat(dto.id()).isEqualTo("fp-1");
        assertThat(dto.goal()).isEqualTo("meta");
        assertThat(dto.recommendations()).containsExactly("passo 1");
        assertThat(dto.provider()).isEqualTo("ollama");
    }
}
