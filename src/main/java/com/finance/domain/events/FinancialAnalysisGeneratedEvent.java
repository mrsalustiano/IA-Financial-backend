package com.finance.domain.events;

import java.time.Instant;

public record FinancialAnalysisGeneratedEvent(String analysisId, String userId, long score, Instant occurredAt)
    implements DomainEvent {

    @Override
    public String aggregateId() {
        return analysisId;
    }
}
