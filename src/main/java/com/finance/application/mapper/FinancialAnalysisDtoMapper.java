package com.finance.application.mapper;

import com.finance.application.command.GenerateFinancialAnalysisCommand;
import com.finance.application.dto.CreateFinancialAnalysisDto;
import com.finance.application.dto.FinancialAnalysisResponseDto;
import com.finance.domain.model.FinancialAnalysis;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialAnalysisDtoMapper {

    GenerateFinancialAnalysisCommand toCommand(CreateFinancialAnalysisDto dto);

    FinancialAnalysisResponseDto toResponseDto(FinancialAnalysis analysis);
}
