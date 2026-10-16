package com.finance.adapters.out.mongodb;

import com.finance.domain.model.AssistantMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssistantMessagePersistenceMapper {

    AssistantMessageDocument toDocument(AssistantMessage message);

    default AssistantMessage toDomain(AssistantMessageDocument document) {
        return AssistantMessage.rehydrate(document.getId(), document.getUserId(), document.getQuestion(),
            document.getAnswer(), document.getProvider(), document.getModel(), document.isDeleted(),
            document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
