package com.finance.adapters.out.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.adapters.out.mongodb.AuditLogDocument;
import com.finance.adapters.out.mongodb.AuditLogMongoRepository;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AuditLogMongoRepository auditLogRepository;

    @Captor
    private ArgumentCaptor<AuditLogDocument> documentCaptor;

    @Test
    void snapshotReturnsSerializedTree() {
        AuditAdapter adapter = new AuditAdapter(auditLogRepository, objectMapper);
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null,
            com.finance.domain.enums.AccountType.DINHEIRO, true, Instant.now());

        Object snapshot = adapter.snapshot(account);

        JsonNode node = (JsonNode) objectMapper.valueToTree(snapshot);
        assertThat(node.isObject()).isTrue();
        assertThat(node.path("id").asText()).isEqualTo("acc-1");
    }

    @Test
    void recordPersistsAuditDocument() {
        AuditAdapter adapter = new AuditAdapter(auditLogRepository, objectMapper);
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null,
            com.finance.domain.enums.AccountType.DINHEIRO, true, Instant.now());

        adapter.record("Account", "acc-1", AuditAction.CRIACAO, null, account);

        verify(auditLogRepository).save(documentCaptor.capture());
        AuditLogDocument document = documentCaptor.getValue();
        assertThat(document.getId()).isNotBlank();
        assertThat(document.getEntityType()).isEqualTo("Account");
        assertThat(document.getEntityId()).isEqualTo("acc-1");
        assertThat(document.getAction()).isEqualTo(AuditAction.CRIACAO);
        assertThat(document.getOldValue()).isNull();
        assertThat(document.getNewValue()).contains("Main");
        assertThat(document.getCreatedAt()).isNotNull();
    }

    @Test
    void recordSerializesOldAndNewValues() {
        AuditAdapter adapter = new AuditAdapter(auditLogRepository, objectMapper);
        Account old = Account.create("acc-1", "LOCAL_USER", "Old", null,
            com.finance.domain.enums.AccountType.DINHEIRO, true, Instant.now());
        Account current = Account.create("acc-1", "LOCAL_USER", "New", null,
            com.finance.domain.enums.AccountType.DINHEIRO, true, Instant.now());

        adapter.record("Account", "acc-1", AuditAction.ATUALIZACAO, old, current);

        verify(auditLogRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getOldValue()).contains("Old");
        assertThat(documentCaptor.getValue().getNewValue()).contains("New");
    }

    @Test
    void toEntryMapsDocument() {
        AuditAdapter adapter = new AuditAdapter(auditLogRepository, objectMapper);
        AuditLogDocument document = new AuditLogDocument("audit-1", "Account", "acc-1", AuditAction.EXCLUSAO,
            "{\"id\":\"acc-1\"}", null, Instant.now());

        var entry = adapter.toEntry(document);

        assertThat(entry.getId()).isEqualTo("audit-1");
        assertThat(entry.getEntityType()).isEqualTo("Account");
        assertThat(entry.getEntityId()).isEqualTo("acc-1");
        assertThat(entry.getAction()).isEqualTo(AuditAction.EXCLUSAO);
        assertThat((String) entry.getOldValue()).contains("acc-1");
        assertThat(entry.getNewValue()).isNull();
    }
}
