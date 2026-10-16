package com.finance.adapters.out.mongodb;

import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Category;
import com.finance.domain.ports.out.CategoryRepositoryPort;
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
import com.finance.domain.ports.out.CurrentUserPort;

@Component
public class CategoryMongoAdapter implements CategoryRepositoryPort {

    private final CategoryMongoRepository repository;
    private final CategoryPersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserPort currentUserPort;

    public CategoryMongoAdapter(CategoryMongoRepository repository, CategoryPersistenceMapper persistenceMapper,
                                MongoTemplate mongoTemplate,
                                   CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Category save(Category category) {
        try {
            CategoryDocument saved = repository.save(persistenceMapper.toDocument(category));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar categoria", ex);
        }
    }

    @Override
    public Optional<Category> findById(String id) {
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
    public PageResult<Category> search(CategorySearchQuery query) {
        try {
            List<Criteria> parts = new ArrayList<>();
            parts.add(Criteria.where("userId").is(currentUserPort.requireUserId()));
            parts.add(Criteria.where("deleted").is(false));
            if (query.classification() != null) {
                parts.add(Criteria.where("classification").is(query.classification().name()));
            }
            if (query.active() != null) {
                parts.add(Criteria.where("active").is(query.active()));
            }
            Pageable pageable = resolvePageable(query);
            long total = mongoTemplate.count(new Query(new Criteria().andOperator(parts)), CategoryDocument.class);
            List<Category> content = mongoTemplate
                .find(new Query(new Criteria().andOperator(parts)).with(pageable), CategoryDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int pageSize = pageSizeOf(pageable);
            int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
            return new PageResult<>(content, pageable.getPageNumber(), pageSize, total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar categorias", ex);
        }
    }

    private Pageable resolvePageable(CategorySearchQuery query) {
        Pageable pageable = query.pageable();
        Sort sort = pageable != null && pageable.getSort().isSorted() ? pageable.getSort()
            : Sort.by(Sort.Direction.ASC, "name");
        int pageNumber = pageable == null || pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        return PageRequest.of(pageNumber, pageSizeOf(pageable), sort);
    }

    private int pageSizeOf(Pageable pageable) {
        return pageable == null || pageable.getPageSize() <= 0 ? 20 : pageable.getPageSize();
    }
}
