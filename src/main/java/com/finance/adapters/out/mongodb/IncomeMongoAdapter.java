package com.finance.adapters.out.mongodb;

import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;
import com.finance.domain.ports.out.IncomeRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class IncomeMongoAdapter implements IncomeRepositoryPort {

    private final IncomeMongoRepository repository;
    private final IncomePersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;

    public IncomeMongoAdapter(IncomeMongoRepository repository, IncomePersistenceMapper persistenceMapper,
                              MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Income save(Income income) {
        try {
            IncomeDocument saved = repository.save(persistenceMapper.toDocument(income));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar receita", ex);
        }
    }

    @Override
    public Optional<Income> findById(String id) {
        return repository.findByIdAndDeletedFalse(id).map(persistenceMapper::toDomain);
    }

    @Override
    public PageResult<Income> search(IncomeSearchQuery query) {
        try {
            List<Criteria> parts = new ArrayList<>();
            parts.add(Criteria.where("deleted").is(false));
            if (query.accountId() != null && !query.accountId().isBlank()) {
                parts.add(Criteria.where("accountId").is(query.accountId()));
            }
            if (query.startDate() != null) {
                parts.add(Criteria.where("receivedAt")
                    .gte(java.util.Date.from(query.startDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant())));
            }
            if (query.endDate() != null) {
                parts.add(Criteria.where("receivedAt")
                    .lt(java.util.Date.from(query.endDate().plusDays(1)
                        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant())));
            }
            Pageable pageable = resolvePageable(query);
            long total = mongoTemplate.count(new Query(new Criteria().andOperator(parts)), IncomeDocument.class);
            List<Income> content = mongoTemplate
                .find(new Query(new Criteria().andOperator(parts)).with(pageable), IncomeDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int totalPages = pageSizeOf(pageable) == 0 ? 0 : (int) Math.ceil((double) total / pageSizeOf(pageable));
            return new PageResult<>(content, pageable.getPageNumber(), pageSizeOf(pageable), total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar receitas", ex);
        }
    }

    private Pageable resolvePageable(IncomeSearchQuery query) {
        Pageable pageable = query.pageable();
        Sort sort = pageable != null && pageable.getSort().isSorted() ? pageable.getSort()
            : Sort.by(Sort.Direction.DESC, "receivedAt");
        int pageNumber = pageable == null || pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        return PageRequest.of(pageNumber, pageSizeOf(pageable), sort);
    }

    private int pageSizeOf(Pageable pageable) {
        return pageable == null || pageable.getPageSize() <= 0 ? 20 : pageable.getPageSize();
    }
}
