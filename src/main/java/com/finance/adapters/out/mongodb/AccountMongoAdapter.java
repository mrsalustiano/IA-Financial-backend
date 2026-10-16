package com.finance.adapters.out.mongodb;

import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import com.finance.domain.ports.out.CurrentUserPort;

@Component
public class AccountMongoAdapter implements AccountRepositoryPort {

    private final AccountMongoRepository repository;
    private final AccountPersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserPort currentUserPort;

    public AccountMongoAdapter(AccountMongoRepository repository, AccountPersistenceMapper persistenceMapper,
                               MongoTemplate mongoTemplate,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Account save(Account account) {
        try {
            AccountDocument saved = repository.save(persistenceMapper.toDocument(account));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar conta", ex);
        }
    }

    @Override
    public Optional<Account> findById(String id) {
        return repository.findByIdAndUserIdAndDeletedFalse(id, currentUserPort.requireUserId()).map(persistenceMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name, String excludeId) {
        if (excludeId == null) {
            return repository.existsByNameIgnoreCaseAndUserIdAndDeletedFalse(name,
                currentUserPort.requireUserId());
        }
        return repository.existsByNameIgnoreCaseAndUserIdAndDeletedFalseAndIdNot(name,
                currentUserPort.requireUserId(), excludeId);
    }

    @Override
    public PageResult<Account> search(AccountSearchQuery query) {
        try {
            Criteria criteria = Criteria.where("userId").is(currentUserPort.requireUserId()).and("deleted").is(false);
            if (query.type() != null) {
                criteria = criteria.and("type").is(query.type().name());
            }
            if (query.active() != null) {
                criteria = criteria.and("active").is(query.active());
            }
            Pageable pageable = resolvePageable(query);
            long total = mongoTemplate.count(new Query(criteria), AccountDocument.class);
            List<Account> content = mongoTemplate.find(new Query(criteria).with(pageable), AccountDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int totalPages = pageSizeOf(pageable) == 0 ? 0 : (int) Math.ceil((double) total / pageSizeOf(pageable));
            return new PageResult<>(content, pageable.getPageNumber(), pageSizeOf(pageable), total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar contas", ex);
        }
    }

    private Pageable resolvePageable(AccountSearchQuery query) {
        Pageable pageable = query.pageable();
        Sort sort = pageable != null && pageable.getSort().isSorted() ? pageable.getSort()
            : Sort.by(Sort.Direction.DESC, "createdAt");
        int pageNumber = pageable == null || pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        return PageRequest.of(pageNumber, pageSizeOf(pageable), sort);
    }

    private int pageSizeOf(Pageable pageable) {
        return pageable == null || pageable.getPageSize() <= 0 ? 20 : pageable.getPageSize();
    }
}
