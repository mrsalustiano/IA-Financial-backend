package com.finance.adapters.out.mongodb;

import com.finance.domain.model.User;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class UserMongoAdapter implements UserRepositoryPort {

    private final UserMongoRepository repository;
    private final UserPersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;

    public UserMongoAdapter(UserMongoRepository repository, UserPersistenceMapper persistenceMapper,
                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public User save(User user) {
        try {
            UserDocument saved = repository.save(persistenceMapper.toDocument(user));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar usuário", ex);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id)
            .filter(document -> !document.isDeleted())
            .map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailIgnoreCaseAndDeletedFalse(email).map(persistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email, String excludeId) {
        if (excludeId == null) {
            return repository.existsByEmailIgnoreCaseAndDeletedFalse(email);
        }
        return repository.existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(email, excludeId);
    }

    @Override
    public boolean existsByCpf(String cpf, String excludeId) {
        if (excludeId == null) {
            return repository.existsByCpfAndDeletedFalse(cpf);
        }
        return repository.existsByCpfAndDeletedFalseAndIdNot(cpf, excludeId);
    }

    @Override
    public List<String> findAllActiveUserIds() {
        try {
            return mongoTemplate.find(new Query(where("deleted").is(false).and("active").is(true)), UserDocument.class)
                .stream()
                .map(UserDocument::getId)
                .toList();
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar usuários ativos", ex);
        }
    }

    @Override
    public com.finance.application.query.PageResult<User> search(
        com.finance.application.query.UserSearchQuery query) {
        try {
            List<Criteria> parts = new java.util.ArrayList<>();
            parts.add(Criteria.where("deleted").is(false));
            if (query.active() != null) {
                parts.add(Criteria.where("active").is(query.active()));
            }
            Query countQuery = new Query(new Criteria().andOperator(parts.toArray(new Criteria[0])));
            long total = mongoTemplate.count(countQuery, UserDocument.class);
            Query pageQuery = countQuery.with(query.pageable());
            List<User> content = mongoTemplate.find(pageQuery, UserDocument.class).stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int requestedSize = Math.max(1, query.pageable().getPageSize());
            int totalPages = (int) Math.ceil(total / (double) requestedSize);
            return new com.finance.application.query.PageResult<>(content, query.pageable().getPageNumber(),
                requestedSize, total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar usuários", ex);
        }
    }
}
