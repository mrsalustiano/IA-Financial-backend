package com.finance.adapters.out.audit;

import com.finance.adapters.out.mongodb.AuditLogDocument;
import com.finance.adapters.out.mongodb.AuditLogMongoRepository;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.AuditEntry;
import com.finance.domain.ports.out.AuditPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.shared.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditAdapter.class);

    private final AuditLogMongoRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditAdapter(AuditLogMongoRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object snapshot(Object value) {
        return objectMapper.valueToTree(value);
    }

    @Override
    public void record(String entityType, String entityId, AuditAction action, Object oldValue, Object newValue) {
        try {
            AuditLogDocument document = new AuditLogDocument(
                com.finance.shared.util.Identifiers.newId(),
                entityType,
                entityId,
                action,
                toJson(oldValue),
                toJson(newValue),
                Instant.now()
            );
            auditLogRepository.save(document);
            log.debug("Audit recorded entityType={} entityId={} action={}", entityType, entityId, action);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao registrar log de auditoria", ex);
        }
    }

    public AuditEntry toEntry(AuditLogDocument document) {
        return new AuditEntry(document.getId(), document.getEntityType(), document.getEntityId(),
            document.getAction(), document.getOldValue(), document.getNewValue(), document.getCreatedAt());
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        JsonNode node = value instanceof String s ? objectMapper.valueToTree(s) : objectMapper.valueToTree(value);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new DatabaseException("Falha ao serializar valor de auditoria", ex);
        }
    }
}
