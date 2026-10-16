package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AssistantMessageMongoRepository extends MongoRepository<AssistantMessageDocument, String> {

    Optional<AssistantMessageDocument> findByIdAndDeletedFalse(String id);
}
