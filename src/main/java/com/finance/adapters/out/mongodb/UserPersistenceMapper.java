package com.finance.adapters.out.mongodb;

import com.finance.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    UserDocument toDocument(User user);

    default User toDomain(UserDocument document) {
        return User.rehydrate(document.getId(), document.getEmail(), document.getCpf(), document.getPasswordHash(),
            document.isActive(), document.isDeleted(), document.getDeletedAt(), document.getCreatedAt(),
            document.getUpdatedAt());
    }
}
