package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FinancialPlanMongoRepository extends MongoRepository<FinancialPlanDocument, String> {

    Optional<FinancialPlanDocument> findByIdAndDeletedFalse(String id);

    Optional<FinancialPlanDocument> findByIdAndUserIdAndDeletedFalse(String id, String userId);
}
