package com.finance.application.mapper;

import com.finance.application.command.CreateAccountCommand;
import com.finance.application.command.UpdateAccountCommand;
import com.finance.application.dto.AccountResponseDto;
import com.finance.application.dto.CreateAccountDto;
import com.finance.application.dto.UpdateAccountDto;
import com.finance.domain.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountDtoMapper {

    CreateAccountCommand toCommand(CreateAccountDto dto);

    UpdateAccountCommand toCommand(UpdateAccountDto dto);

    AccountResponseDto toResponseDto(Account account);
}
