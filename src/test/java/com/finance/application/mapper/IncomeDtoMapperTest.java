package com.finance.application.mapper;

import com.finance.application.command.CreateIncomeCommand;
import com.finance.application.command.UpdateIncomeCommand;
import com.finance.application.dto.CreateIncomeDto;
import com.finance.application.dto.IncomeResponseDto;
import com.finance.application.dto.UpdateIncomeDto;
import com.finance.domain.model.Income;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeDtoMapperTest {

    private final IncomeDtoMapper mapper = Mappers.getMapper(IncomeDtoMapper.class);

    @Test
    void mapsCreateDtoToCommand() {
        CreateIncomeCommand command = mapper.toCommand(new CreateIncomeDto("acc-1", "Salary",
            new BigDecimal("99.99"), LocalDate.of(2026, 10, 5)));

        assertThat(command.accountId()).isEqualTo("acc-1");
        assertThat(command.description()).isEqualTo("Salary");
        assertThat(command.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(command.receivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void mapsUpdateDtoToCommand() {
        UpdateIncomeCommand command = mapper.toCommand(new UpdateIncomeDto("acc-2", "Bonus",
            new BigDecimal("1"), LocalDate.of(2026, 10, 6)));

        assertThat(command.accountId()).isEqualTo("acc-2");
        assertThat(command.description()).isEqualTo("Bonus");
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(command.receivedAt()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    void mapsDomainToResponseDto() {
        Income income = Income.create("inc-1", "LOCAL_USER", "acc-1", "Salary", new BigDecimal("2500.00"),
            LocalDate.of(2026, 10, 5), Instant.parse("2026-10-01T10:00:00Z"));

        IncomeResponseDto dto = mapper.toResponseDto(income);

        assertThat(dto.id()).isEqualTo("inc-1");
        assertThat(dto.userId()).isEqualTo("LOCAL_USER");
        assertThat(dto.accountId()).isEqualTo("acc-1");
        assertThat(dto.description()).isEqualTo("Salary");
        assertThat(dto.amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(dto.receivedAt()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(dto.createdAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(dto.updatedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
    }
}
