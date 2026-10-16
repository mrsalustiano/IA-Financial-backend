package com.finance.application.validator;

import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountValidatorTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Test
    void passesWhenNameIsAvailable() {
        when(accountRepositoryPort.existsByName("Main", null)).thenReturn(false);

        assertThatCode(() -> new AccountValidator(accountRepositoryPort).ensureNameAvailable("Main", null))
            .doesNotThrowAnyException();
    }

    @Test
    void throwsConflictWhenNameExistsOnCreate() {
        when(accountRepositoryPort.existsByName("Main", null)).thenReturn(true);

        assertThatThrownBy(() -> new AccountValidator(accountRepositoryPort).ensureNameAvailable("Main", null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Main");
    }

    @Test
    void excludesCurrentIdOnUpdate() {
        when(accountRepositoryPort.existsByName("Main", "acc-1")).thenReturn(false);

        new AccountValidator(accountRepositoryPort).ensureNameAvailable("Main", "acc-1");

        verify(accountRepositoryPort).existsByName("Main", "acc-1");
    }
}
