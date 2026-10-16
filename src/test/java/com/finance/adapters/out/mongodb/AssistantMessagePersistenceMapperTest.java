package com.finance.adapters.out.mongodb;

import com.finance.domain.model.AssistantMessage;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantMessagePersistenceMapperTest {

    private final AssistantMessagePersistenceMapper mapper = Mappers.getMapper(AssistantMessagePersistenceMapper.class);

    @Test
    void mapsBothWays() {
        Instant now = Instant.now();
        AssistantMessage message = AssistantMessage.create("am-1", "LOCAL_USER", "q", "a", "ollama", "llama3", now);
        AssistantMessageDocument document = mapper.toDocument(message);
        assertThat(document.getQuestion()).isEqualTo("q");
        AssistantMessage domain = mapper.toDomain(document);
        assertThat(domain.getAnswer()).isEqualTo("a");
    }
}
