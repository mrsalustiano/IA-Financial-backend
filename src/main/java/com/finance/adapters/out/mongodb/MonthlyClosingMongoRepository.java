package com.finance.adapters.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyClosingMongoRepository extends MongoRepository<MonthlyClosingDocument, String> {

    Optional<MonthlyClosingDocument> findByIdAndUserIdAndDeletedFalse(String id, String userId);

    Optional<MonthlyClosingDocument> findByUserIdAndReferenceMonthAndDeletedFalse(String userId,
                                                                                  String referenceMonth);

    List<MonthlyClosingDocument> findByUserIdAndDeletedFalseOrderByReferenceMonthDesc(String userId);
}
