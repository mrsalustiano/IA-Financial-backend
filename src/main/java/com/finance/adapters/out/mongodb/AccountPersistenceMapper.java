package com.finance.adapters.out.mongodb;

import com.finance.domain.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountPersistenceMapper {

    AccountDocument toDocument(Account account);

    default Account toDomain(AccountDocument document) {
        return Account.rehydrate(document.getId(), document.getUserId(), document.getName(), document.getDescription(),
            document.getType(), document.isActive(), document.isDeleted(), document.getDeletedAt(),
            document.getCreatedAt(), document.getUpdatedAt());
    }
}
