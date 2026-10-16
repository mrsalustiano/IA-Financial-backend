package com.finance.adapters.out.mongodb;

import com.finance.domain.model.MonthlyClosing;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MonthlyClosingPersistenceMapper {

    MonthlyClosingDocument toDocument(MonthlyClosing closing);

    default MonthlyClosing toDomain(MonthlyClosingDocument document) {
        return MonthlyClosing.rehydrate(document.getId(), document.getUserId(), document.getReferenceMonth(),
            document.getTotalIncome(), document.getTotalExpense(), document.getBalance(), document.getClosedAt(),
            document.isDeleted(), document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
