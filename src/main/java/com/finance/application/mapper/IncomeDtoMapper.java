package com.finance.application.mapper;

import com.finance.application.command.CreateIncomeCommand;
import com.finance.application.command.UpdateIncomeCommand;
import com.finance.application.dto.CreateIncomeDto;
import com.finance.application.dto.IncomeResponseDto;
import com.finance.application.dto.UpdateIncomeDto;
import com.finance.domain.model.Income;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncomeDtoMapper {

    CreateIncomeCommand toCommand(CreateIncomeDto dto);

    UpdateIncomeCommand toCommand(UpdateIncomeDto dto);

    IncomeResponseDto toResponseDto(Income income);
}
