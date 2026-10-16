package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategoryMongoRepository extends MongoRepository<CategoryDocument, String> {

    Optional<CategoryDocument> findByIdAndDeletedFalse(String id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, String id);

    Optional<CategoryDocument> findByIdAndUserIdAndDeletedFalse(String id, String userId);

    boolean existsByNameIgnoreCaseAndUserIdAndDeletedFalse(String name, String userId);

    boolean existsByNameIgnoreCaseAndUserIdAndDeletedFalseAndIdNot(String name, String userId, String id);
}
