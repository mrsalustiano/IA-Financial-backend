package com.finance.domain.events;

import java.time.Instant;

public interface DomainEvent {

    String aggregateId();

    Instant occurredAt();
}
