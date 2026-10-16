package com.finance.domain.events;

import java.time.Instant;

public record UserRegisteredEvent(String userId, String email, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return userId;
    }
}
