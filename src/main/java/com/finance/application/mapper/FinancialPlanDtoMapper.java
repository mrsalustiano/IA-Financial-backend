package com.finance.application.mapper;

import com.finance.application.command.CreateFinancialPlanCommand;
import com.finance.application.dto.CreateFinancialPlanDto;
import com.finance.application.dto.FinancialPlanResponseDto;
import com.finance.domain.model.FinancialPlan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialPlanDtoMapper {

    CreateFinancialPlanCommand toCommand(CreateFinancialPlanDto dto);

    FinancialPlanResponseDto toResponseDto(FinancialPlan plan);
}
