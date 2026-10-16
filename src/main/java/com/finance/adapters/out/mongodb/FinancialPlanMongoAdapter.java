package com.finance.adapters.out.mongodb;

import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialPlan;
import com.finance.domain.ports.out.FinancialPlanRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import com.finance.domain.ports.out.CurrentUserPort;

@Component
public class FinancialPlanMongoAdapter implements FinancialPlanRepositoryPort {

    private final FinancialPlanMongoRepository repository;
    private final FinancialPlanPersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserPort currentUserPort;

    public FinancialPlanMongoAdapter(FinancialPlanMongoRepository repository,
                                     FinancialPlanPersistenceMapper persistenceMapper, MongoTemplate mongoTemplate,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public FinancialPlan save(FinancialPlan plan) {
        try {
            FinancialPlanDocument saved = repository.save(persistenceMapper.toDocument(plan));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar plano financeiro", ex);
        }
    }

    @Override
    public Optional<FinancialPlan> findById(String id) {
        return repository.findByIdAndUserIdAndDeletedFalse(id, currentUserPort.requireUserId()).map(persistenceMapper::toDomain);
    }

    @Override
    public PageResult<FinancialPlan> search(int page, int size) {
        try {
            int pageNumber = Math.max(page, 0);
            int pageSize = size <= 0 ? 20 : size;
            long total = mongoTemplate.count(Query.query(scopedCriteria()),
                FinancialPlanDocument.class);
            List<FinancialPlan> content = mongoTemplate
                .find(new Query(scopedCriteria())
                    .with(PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))),
                    FinancialPlanDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PageResult<>(content, pageNumber, pageSize, total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar planos financeiros", ex);
        }
    }

    private Criteria scopedCriteria() {
        return Criteria.where("userId").is(currentUserPort.requireUserId())
            .and("deleted").is(false);
    }
}