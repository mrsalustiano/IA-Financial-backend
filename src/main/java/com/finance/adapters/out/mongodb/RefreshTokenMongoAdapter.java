package com.finance.adapters.out.mongodb;

import com.finance.domain.model.RefreshToken;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.shared.exception.DatabaseException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenMongoAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenMongoRepository repository;

    public RefreshTokenMongoAdapter(RefreshTokenMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        try {
            RefreshTokenDocument document = toDocument(token);
            RefreshTokenDocument saved = repository.save(document);
            return toDomain(saved);
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao salvar refresh token", ex);
        }
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHashAndDeletedFalse(tokenHash).map(this::toDomain);
    }

    private RefreshTokenDocument toDocument(RefreshToken token) {
        return new RefreshTokenDocument(token.getId(), token.getUserId(), token.getTokenHash(), token.getExpiresAt(),
            token.isRevoked(), token.getRevokedAt(), token.isDeleted(), token.getDeletedAt(), token.getCreatedAt(),
            token.getUpdatedAt());
    }

    private RefreshToken toDomain(RefreshTokenDocument document) {
        return RefreshToken.rehydrate(document.getId(), document.getUserId(), document.getTokenHash(),
            document.getExpiresAt(), document.isRevoked(), document.getRevokedAt(), document.isDeleted(),
            document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
