package com.finance.domain.ports.out;

import com.finance.domain.enums.AuditAction;

public interface AuditPort {

    Object snapshot(Object value);

    void record(String entityType, String entityId, AuditAction action, Object oldValue, Object newValue);
}
