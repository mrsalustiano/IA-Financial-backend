package com.finance.domain.model;

import com.finance.domain.enums.AccountType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void createSetsDefaultsAndTimestamps() {
        Instant now = Instant.now();

        Account account = Account.create("acc-1", "LOCAL_USER", "Main", "Desc", AccountType.CONTA_CORRENTE, false, now);

        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(account.getName()).isEqualTo("Main");
        assertThat(account.getDescription()).isEqualTo("Desc");
        assertThat(account.getType()).isEqualTo(AccountType.CONTA_CORRENTE);
        assertThat(account.isActive()).isFalse();
        assertThat(account.isDeleted()).isFalse();
        assertThat(account.getDeletedAt()).isNull();
        assertThat(account.getCreatedAt()).isEqualTo(now);
        assertThat(account.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void mutatingMethodsChangeOnlyExpectedState() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.DINHEIRO, true, Instant.now());

        account.rename("Renamed");
        account.describe("With description");
        account.changeType(AccountType.POUPANCA);
        account.deactivate();
        Instant later = Instant.now();
        account.touch(later);
        assertThat(account.getName()).isEqualTo("Renamed");
        assertThat(account.getDescription()).isEqualTo("With description");
        assertThat(account.getType()).isEqualTo(AccountType.POUPANCA);
        assertThat(account.isActive()).isFalse();
        assertThat(account.getUpdatedAt()).isEqualTo(later);
        assertThat(account.isDeleted()).isFalse();

        account.activate();
        assertThat(account.isActive()).isTrue();
    }

    @Test
    void markDeletedSetsDeletedFieldsAndUpdatedAt() {
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.DINHEIRO, true, Instant.now());
        Instant later = Instant.now();

        account.markDeleted(later);

        assertThat(account.isDeleted()).isTrue();
        assertThat(account.getDeletedAt()).isEqualTo(later);
        assertThat(account.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void rehydrateRestoresFullState() {
        Instant createdAt = Instant.now();
        Instant deletedAt = createdAt.plusSeconds(60);

        Account account = Account.rehydrate("acc-1", "LOCAL_USER", "Main", "Desc", AccountType.INVESTIMENTO, false,
            true, deletedAt, createdAt, deletedAt);

        assertThat(account.getType()).isEqualTo(AccountType.INVESTIMENTO);
        assertThat(account.isActive()).isFalse();
        assertThat(account.isDeleted()).isTrue();
        assertThat(account.getDeletedAt()).isEqualTo(deletedAt);
    }
}
