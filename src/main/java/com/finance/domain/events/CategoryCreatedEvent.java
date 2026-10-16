package com.finance.domain.events;

import java.time.Instant;

public record CategoryCreatedEvent(String categoryId, String userId, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return categoryId;
    }
}
