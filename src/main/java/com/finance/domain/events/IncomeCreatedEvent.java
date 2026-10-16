package com.finance.domain.events;

import java.time.Instant;

public record IncomeCreatedEvent(String incomeId, String userId, String accountId, Instant occurredAt)
    implements DomainEvent {

    @Override
    public String aggregateId() {
        return incomeId;
    }
}
