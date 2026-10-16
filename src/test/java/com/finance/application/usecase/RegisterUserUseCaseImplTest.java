package com.finance.application.usecase;

import com.finance.application.command.RegisterUserCommand;
import com.finance.application.validator.UserValidator;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.UserRegisteredEvent;
import com.finance.domain.model.User;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.PasswordHasherPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private UserValidator userValidator;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private RegisterUserUseCaseImpl useCase;

    @Test
    void registersActiveUserWithHashedPasswordAndAudit() {
        when(passwordHasherPort.hash("Senha@123")).thenReturn("bcrypt-hash");
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = useCase.handle(new RegisterUserCommand("A@Test.com ", "529.982.247-25", "Senha@123", null));

        assertThat(user.getEmail()).isEqualTo("a@test.com");
        assertThat(user.getCpf()).isEqualTo("52998224725");
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.isActive()).isTrue();
        verify(userValidator).ensureRegistrable(any(User.class), eq("Senha@123"));
        verify(auditPort).record(eq("User"), eq(user.getId()), eq(AuditAction.CRIACAO), isNull(), any());
        ArgumentCaptor<UserRegisteredEvent> event = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().email()).isEqualTo("a@test.com");
    }

    @Test
    void registersInactiveUserWhenRequested() {
        when(passwordHasherPort.hash(any())).thenReturn("hash");
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = useCase.handle(new RegisterUserCommand("b@test.com", "52998224725", "Senha@123", false));

        assertThat(user.isActive()).isFalse();
    }
}
