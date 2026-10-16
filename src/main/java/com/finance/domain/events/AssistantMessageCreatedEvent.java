package com.finance.domain.events;

import java.time.Instant;

public record AssistantMessageCreatedEvent(String messageId, String userId, Instant occurredAt)
    implements DomainEvent {

    @Override
    public String aggregateId() {
        return messageId;
    }
}
