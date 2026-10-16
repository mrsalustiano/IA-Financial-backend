package com.finance.adapters.out.mongodb;

import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPersistenceMapperTest {

    private final AccountPersistenceMapper mapper = Mappers.getMapper(AccountPersistenceMapper.class);

    @Test
    void mapsDomainToDocument() {
        Instant now = Instant.now();
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", "Desc", AccountType.CARTAO_CREDITO, true, now);

        AccountDocument document = mapper.toDocument(account);

        assertThat(document.getId()).isEqualTo("acc-1");
        assertThat(document.getUserId()).isEqualTo("LOCAL_USER");
        assertThat(document.getName()).isEqualTo("Main");
        assertThat(document.getDescription()).isEqualTo("Desc");
        assertThat(document.getType()).isEqualTo(AccountType.CARTAO_CREDITO);
        assertThat(document.isActive()).isTrue();
        assertThat(document.isDeleted()).isFalse();
        assertThat(document.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void mapsDocumentToDomain() {
        Instant now = Instant.now();
        AccountDocument document = new AccountDocument("acc-1", "LOCAL_USER", "Main", "Desc", AccountType.POUPANCA,
            false, true, now, now, now);

        Account account = mapper.toDomain(document);

        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getType()).isEqualTo(AccountType.POUPANCA);
        assertThat(account.isActive()).isFalse();
        assertThat(account.isDeleted()).isTrue();
        assertThat(account.getDeletedAt()).isEqualTo(now);
    }
}
