package com.finance.application.mapper;

import com.finance.application.command.CreateAccountCommand;
import com.finance.application.command.UpdateAccountCommand;
import com.finance.application.dto.AccountResponseDto;
import com.finance.application.dto.CreateAccountDto;
import com.finance.application.dto.UpdateAccountDto;
import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDtoMapperTest {

    private final AccountDtoMapper mapper = Mappers.getMapper(AccountDtoMapper.class);

    @Test
    void mapsCreateDtoToCommand() {
        CreateAccountCommand command = mapper.toCommand(new CreateAccountDto("Main", "Desc", AccountType.DINHEIRO, true));

        assertThat(command.name()).isEqualTo("Main");
        assertThat(command.description()).isEqualTo("Desc");
        assertThat(command.type()).isEqualTo(AccountType.DINHEIRO);
        assertThat(command.active()).isTrue();
    }

    @Test
    void mapsUpdateDtoToCommand() {
        UpdateAccountCommand command = mapper.toCommand(new UpdateAccountDto("Main", null, AccountType.POUPANCA, false));

        assertThat(command.name()).isEqualTo("Main");
        assertThat(command.description()).isNull();
        assertThat(command.type()).isEqualTo(AccountType.POUPANCA);
        assertThat(command.active()).isFalse();
    }

    @Test
    void mapsDomainToResponseDto() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", "Desc", AccountType.INVESTIMENTO, true,
            Instant.parse("2026-10-01T10:00:00Z"));

        AccountResponseDto dto = mapper.toResponseDto(account);

        assertThat(dto.id()).isEqualTo("acc-1");
        assertThat(dto.userId()).isEqualTo("LOCAL_USER");
        assertThat(dto.name()).isEqualTo("Main");
        assertThat(dto.description()).isEqualTo("Desc");
        assertThat(dto.type()).isEqualTo(AccountType.INVESTIMENTO);
        assertThat(dto.active()).isTrue();
        assertThat(dto.createdAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(dto.updatedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
    }
}
