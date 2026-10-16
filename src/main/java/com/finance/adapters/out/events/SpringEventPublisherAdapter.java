package com.finance.adapters.out.events;

import com.finance.domain.events.DomainEvent;
import com.finance.domain.ports.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisherAdapter.class);

    private final ApplicationEventPublisher eventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }
}
