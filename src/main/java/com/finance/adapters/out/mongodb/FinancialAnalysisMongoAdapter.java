package com.finance.adapters.out.mongodb;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.ports.out.FinancialAnalysisRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FinancialAnalysisMongoAdapter implements FinancialAnalysisRepositoryPort {

    private final FinancialAnalysisMongoRepository repository;
    private final FinancialAnalysisPersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;

    public FinancialAnalysisMongoAdapter(FinancialAnalysisMongoRepository repository,
                                         FinancialAnalysisPersistenceMapper persistenceMapper,
                                         MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public FinancialAnalysis save(FinancialAnalysis analysis) {
        try {
            FinancialAnalysisDocument saved = repository.save(persistenceMapper.toDocument(analysis));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar análise", ex);
        }
    }

    @Override
    public Optional<FinancialAnalysis> findById(String id) {
        return repository.findByIdAndDeletedFalse(id).map(persistenceMapper::toDomain);
    }

    @Override
    public PageResult<FinancialAnalysis> search(int page, int size) {
        try {
            int pageNumber = Math.max(page, 0);
            int pageSize = size <= 0 ? 20 : size;
            Query query = new Query(Criteria.where("deleted").is(false))
                .with(PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
            long total = mongoTemplate.count(Query.query(Criteria.where("deleted").is(false)),
                FinancialAnalysisDocument.class);
            List<FinancialAnalysis> content = mongoTemplate.find(query, FinancialAnalysisDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PageResult<>(content, pageNumber, pageSize, total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar análises", ex);
        }
    }
}
