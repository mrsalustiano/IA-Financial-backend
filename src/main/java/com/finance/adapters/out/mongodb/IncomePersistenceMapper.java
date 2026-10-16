package com.finance.adapters.out.mongodb;

import com.finance.domain.model.Income;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncomePersistenceMapper {

    IncomeDocument toDocument(Income income);

    default Income toDomain(IncomeDocument document) {
        return Income.rehydrate(document.getId(), document.getUserId(), document.getAccountId(),
            document.getDescription(), document.getAmount(), document.getReceivedAt(), document.isDeleted(),
            document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
