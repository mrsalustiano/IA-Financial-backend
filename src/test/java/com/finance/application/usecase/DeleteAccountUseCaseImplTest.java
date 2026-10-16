package com.finance.application.usecase;

import com.finance.domain.enums.AccountType;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.ports.out.AuditPort;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAccountUseCaseImplTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private DeleteAccountUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Test
    void softDeletesAccountAndAudits() {
        Account existing = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true,
            java.time.Instant.now());
        when(accountRepositoryPort.findById("acc-1")).thenReturn(Optional.of(existing));
        when(auditPort.snapshot(any())).thenReturn("snapshot");

        useCase.handle("acc-1");

        verify(accountRepositoryPort).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertThat(saved.isDeleted()).isTrue();
        assertThat(saved.getDeletedAt()).isNotNull();

        verify(auditPort).record(eq("Account"), eq("acc-1"), eq(AuditAction.EXCLUSAO), eq("snapshot"), isNull());
    }

    @Test
    void throwsNotFoundWhenAccountMissing() {
        when(accountRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing"))
            .isInstanceOf(NotFoundException.class);

        verify(accountRepositoryPort, org.mockito.Mockito.never()).save(any());
    }
}
