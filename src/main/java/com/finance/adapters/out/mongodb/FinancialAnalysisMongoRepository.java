package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FinancialAnalysisMongoRepository extends MongoRepository<FinancialAnalysisDocument, String> {

    Optional<FinancialAnalysisDocument> findByIdAndDeletedFalse(String id);
}
