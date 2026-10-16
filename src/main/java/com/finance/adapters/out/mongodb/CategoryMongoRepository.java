package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategoryMongoRepository extends MongoRepository<CategoryDocument, String> {

    Optional<CategoryDocument> findByIdAndDeletedFalse(String id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, String id);
}
