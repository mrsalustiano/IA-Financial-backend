package com.finance.application.mapper;

import com.finance.application.command.AskAssistantCommand;
import com.finance.application.dto.AskAssistantDto;
import com.finance.application.dto.AssistantMessageResponseDto;
import com.finance.domain.model.AssistantMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssistantMessageDtoMapper {

    AskAssistantCommand toCommand(AskAssistantDto dto);

    AssistantMessageResponseDto toResponseDto(AssistantMessage message);
}
