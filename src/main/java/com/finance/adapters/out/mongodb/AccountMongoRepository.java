package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountMongoRepository extends MongoRepository<AccountDocument, String> {

    java.util.Optional<AccountDocument> findByIdAndDeletedFalse(String id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, String id);

    java.util.Optional<AccountDocument> findByIdAndUserIdAndDeletedFalse(String id, String userId);

    boolean existsByNameIgnoreCaseAndUserIdAndDeletedFalse(String name, String userId);

    boolean existsByNameIgnoreCaseAndUserIdAndDeletedFalseAndIdNot(String name, String userId, String id);
}
