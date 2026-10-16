package com.finance.application.usecase;

import com.finance.application.command.UpdateAccountCommand;
import com.finance.application.validator.AccountValidator;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAccountUseCaseImplTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private AccountValidator accountValidator;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private UpdateAccountUseCaseImpl useCase;

    @Test
    void updatesAllFieldsAndAudits() {
        Account existing = Account.create("acc-1", "LOCAL_USER", "Old", "Old desc", AccountType.DINHEIRO, true,
            java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(accountRepositoryPort.findById("acc-1")).thenReturn(Optional.of(existing));
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditPort.snapshot(any())).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules().valueToTree(existing));

        Account result = useCase.handle("acc-1",
            new UpdateAccountCommand("New", "New desc", AccountType.POUPANCA, false));

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("New desc");
        assertThat(result.getType()).isEqualTo(AccountType.POUPANCA);
        assertThat(result.isActive()).isFalse();
        assertThat(result.getUpdatedAt()).isAfter(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        verify(accountValidator).ensureNameAvailable("New", "acc-1");
        verify(accountRepositoryPort).save(existing);
        ArgumentCaptor<AuditAction> actionCaptor = ArgumentCaptor.forClass(AuditAction.class);
        verify(auditPort).record(org.mockito.ArgumentMatchers.eq("Account"),
            org.mockito.ArgumentMatchers.eq("acc-1"), actionCaptor.capture(), any(), any());
        assertThat(actionCaptor.getValue()).isEqualTo(AuditAction.ATUALIZACAO);
    }

    @Test
    void throwsNotFoundWhenAccountMissing() {
        when(accountRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle("missing",
            new UpdateAccountCommand("New", null, AccountType.DINHEIRO, true)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("missing");
    }
}
