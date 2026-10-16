package com.finance.application.usecase;

import com.finance.application.command.CreateAccountCommand;
import com.finance.application.validator.AccountValidator;
import com.finance.domain.enums.AccountType;
import com.finance.domain.events.AccountCreatedEvent;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.shared.exception.ConflictException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.finance.domain.ports.out.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseImplTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private AccountValidator accountValidator;

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private CreateAccountUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Mock
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void stubCurrentUser() {
        org.mockito.Mockito.lenient().when(currentUserPort.requireUserId())
            .thenReturn(com.finance.shared.constants.AppConstants.LOCAL_USER_ID);
    }

    @Test
    void createsAccountWithDefaultsAndPublishesEvent() {
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = useCase.handle(new CreateAccountCommand("Main", "Desc", AccountType.CONTA_CORRENTE, null));

        verify(accountValidator).ensureNameAvailable("Main", null);
        verify(accountRepositoryPort).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(saved.getName()).isEqualTo("Main");
        assertThat(saved.getType()).isEqualTo(AccountType.CONTA_CORRENTE);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());

        verify(auditPort).record(eq("Account"), eq(saved.getId()), any(), isNull(), eq(saved));
        ArgumentCaptor<AccountCreatedEvent> eventCaptor = ArgumentCaptor.forClass(AccountCreatedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(saved.getId());
        assertThat(result).isSameAs(saved);
    }

    @Test
    void createsInactiveAccountWhenActiveFlagIsFalse() {
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.handle(new CreateAccountCommand("Idle", null, AccountType.DINHEIRO, false));

        verify(accountRepositoryPort).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().isActive()).isFalse();
    }

    @Test
    void rejectsDuplicatedNameWithoutSaving() {
        org.mockito.Mockito.doThrow(new ConflictException("Já existe uma conta com o nome: Main"))
            .when(accountValidator).ensureNameAvailable("Main", null);

        assertThatThrownBy(() -> useCase.handle(new CreateAccountCommand("Main", null, AccountType.DINHEIRO, null)))
            .isInstanceOf(ConflictException.class);

        verify(accountRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void propagatesValidationExceptionWhenNameInvalid() {
        org.mockito.Mockito.doThrow(new ValidationException("Nome da conta é obrigatório"))
            .when(accountValidator).ensureNameAvailable(isNull(), isNull());

        assertThatThrownBy(() -> useCase.handle(new CreateAccountCommand(null, null, AccountType.DINHEIRO, null)))
            .isInstanceOf(ValidationException.class);

        verify(accountRepositoryPort, never()).save(any());
    }
}
