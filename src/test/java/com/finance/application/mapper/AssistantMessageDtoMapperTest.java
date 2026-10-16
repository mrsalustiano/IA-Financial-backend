package com.finance.application.mapper;

import com.finance.application.command.AskAssistantCommand;
import com.finance.application.dto.AskAssistantDto;
import com.finance.domain.model.AssistantMessage;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantMessageDtoMapperTest {

    private final AssistantMessageDtoMapper mapper = Mappers.getMapper(AssistantMessageDtoMapper.class);

    @Test
    void mapsAskDtoToCommand() {
        AskAssistantCommand command = mapper.toCommand(new AskAssistantDto("como economizar?"));
        assertThat(command.question()).isEqualTo("como economizar?");
    }

    @Test
    void mapsDomainToResponseDto() {
        AssistantMessage message = AssistantMessage.create("am-1", "LOCAL_USER", "q", "a", "ollama", "llama3",
            Instant.now());
        var dto = mapper.toResponseDto(message);
        assertThat(dto.id()).isEqualTo("am-1");
        assertThat(dto.question()).isEqualTo("q");
        assertThat(dto.answer()).isEqualTo("a");
    }
}
