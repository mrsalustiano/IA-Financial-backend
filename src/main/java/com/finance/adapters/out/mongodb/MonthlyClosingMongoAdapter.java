package com.finance.adapters.out.mongodb;

import com.finance.domain.model.MonthlyClosing;
import com.finance.domain.ports.out.MonthlyClosingRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MonthlyClosingMongoAdapter implements MonthlyClosingRepositoryPort {

    private final MonthlyClosingMongoRepository repository;
    private final MonthlyClosingPersistenceMapper persistenceMapper;

    public MonthlyClosingMongoAdapter(MonthlyClosingMongoRepository repository,
                                      MonthlyClosingPersistenceMapper persistenceMapper) {
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
    }

    @Override
    public MonthlyClosing save(MonthlyClosing closing) {
        try {
            MonthlyClosingDocument saved = repository.save(persistenceMapper.toDocument(closing));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar fechamento mensal", ex);
        }
    }

    @Override
    public Optional<MonthlyClosing> findByIdAndUser(String userId, String id) {
        return repository.findByIdAndUserIdAndDeletedFalse(id, userId).map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<MonthlyClosing> findByUserAndReferenceMonth(String userId, String referenceMonth) {
        return repository.findByUserIdAndReferenceMonthAndDeletedFalse(userId, referenceMonth)
            .map(persistenceMapper::toDomain);
    }

    @Override
    public List<MonthlyClosing> findAllByUser(String userId) {
        return repository.findByUserIdAndDeletedFalseOrderByReferenceMonthDesc(userId).stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }
}
