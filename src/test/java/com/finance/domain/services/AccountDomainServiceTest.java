package com.finance.domain.services;

import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.BusinessException;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDomainServiceTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Test
    void returnsAccountWhenExistsAndActive() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.findById("acc-1")).thenReturn(Optional.of(account));
        AccountDomainService service = new AccountDomainService(accountRepositoryPort);

        Account result = service.requireActiveAccount("acc-1");

        assertThat(result).isSameAs(account);
    }

    @Test
    void throwsNotFoundWhenAccountDoesNotExist() {
        when(accountRepositoryPort.findById("ghost")).thenReturn(Optional.empty());
        AccountDomainService service = new AccountDomainService(accountRepositoryPort);

        assertThatThrownBy(() -> service.requireActiveAccount("ghost"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void throwsBusinessExceptionWhenAccountInactive() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Idle", null, AccountType.CONTA_CORRENTE, false, Instant.now());
        when(accountRepositoryPort.findById("acc-1")).thenReturn(Optional.of(account));
        AccountDomainService service = new AccountDomainService(accountRepositoryPort);

        assertThatThrownBy(() -> service.requireActiveAccount("acc-1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inativa");
    }
}
