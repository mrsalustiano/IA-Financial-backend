package com.finance.adapters.out.mongodb;

import com.finance.application.query.PageResult;
import com.finance.domain.model.AssistantMessage;
import com.finance.domain.ports.out.AssistantMessageRepositoryPort;
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
public class AssistantMessageMongoAdapter implements AssistantMessageRepositoryPort {

    private final AssistantMessageMongoRepository repository;
    private final AssistantMessagePersistenceMapper persistenceMapper;
    private final MongoTemplate mongoTemplate;

    public AssistantMessageMongoAdapter(AssistantMessageMongoRepository repository,
                                        AssistantMessagePersistenceMapper persistenceMapper,
                                        MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.persistenceMapper = persistenceMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public AssistantMessage save(AssistantMessage message) {
        try {
            AssistantMessageDocument saved = repository.save(persistenceMapper.toDocument(message));
            return persistenceMapper.toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar mensagem", ex);
        }
    }

    @Override
    public Optional<AssistantMessage> findById(String id) {
        return repository.findByIdAndDeletedFalse(id).map(persistenceMapper::toDomain);
    }

    @Override
    public PageResult<AssistantMessage> search(int page, int size) {
        try {
            int pageNumber = Math.max(page, 0);
            int pageSize = size <= 0 ? 20 : size;
            long total = mongoTemplate.count(Query.query(Criteria.where("deleted").is(false)),
                AssistantMessageDocument.class);
            List<AssistantMessage> content = mongoTemplate
                .find(new Query(Criteria.where("deleted").is(false))
                    .with(PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))),
                    AssistantMessageDocument.class)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PageResult<>(content, pageNumber, pageSize, total, totalPages);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao buscar mensagens", ex);
        }
    }
}
