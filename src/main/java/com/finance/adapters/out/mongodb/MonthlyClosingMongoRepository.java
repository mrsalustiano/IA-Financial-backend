package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyClosingMongoRepository extends MongoRepository<MonthlyClosingDocument, String> {

    Optional<MonthlyClosingDocument> findByIdAndDeletedFalse(String id);

    Optional<MonthlyClosingDocument> findByReferenceMonthAndDeletedFalse(String referenceMonth);

    List<MonthlyClosingDocument> findByDeletedFalseOrderByReferenceMonthDesc();
}
