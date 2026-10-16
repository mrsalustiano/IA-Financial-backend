package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IncomeMongoRepository extends MongoRepository<IncomeDocument, String> {

    java.util.Optional<IncomeDocument> findByIdAndDeletedFalse(String id);
}
