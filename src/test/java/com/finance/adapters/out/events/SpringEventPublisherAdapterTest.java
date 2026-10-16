package com.finance.adapters.out.events;

import com.finance.domain.events.AccountCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<AccountCreatedEvent> eventCaptor;

    @Test
    void publishDelegatesToApplicationContext() {
        SpringEventPublisherAdapter adapter = new SpringEventPublisherAdapter(applicationEventPublisher);
        AccountCreatedEvent event = new AccountCreatedEvent("acc-1", "LOCAL_USER", Instant.now());

        adapter.publish(event);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isSameAs(event);
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo("acc-1");
    }
}
