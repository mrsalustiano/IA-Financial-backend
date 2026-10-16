package com.finance.domain.ports.out;

import com.finance.domain.events.DomainEvent;

public interface EventPublisherPort {

    void publish(DomainEvent event);
}
