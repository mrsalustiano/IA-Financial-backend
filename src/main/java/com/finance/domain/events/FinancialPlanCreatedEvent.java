package com.finance.domain.events;

import java.time.Instant;

public record FinancialPlanCreatedEvent(String planId, String userId, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return planId;
    }
}
