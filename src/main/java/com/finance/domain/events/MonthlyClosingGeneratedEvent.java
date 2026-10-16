package com.finance.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyClosingGeneratedEvent(String closingId, String userId, String referenceMonth,
                                           BigDecimal balance, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return closingId;
    }
}
