package com.finance.application.usecase;

import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountByIdUseCaseImplTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @InjectMocks
    private GetAccountByIdUseCaseImpl useCase;

    @Test
    void returnsAccountWhenFound() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true, Instant.now());
        when(accountRepositoryPort.findById("acc-1")).thenReturn(Optional.of(account));

        Account result = useCase.handle("acc-1");

        assertThat(result).isSameAs(account);
    }

    @Test
    void throwsNotFoundWhenMissing() {
        when(accountRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing"))
            .isInstanceOf(NotFoundException.class);
    }
}
