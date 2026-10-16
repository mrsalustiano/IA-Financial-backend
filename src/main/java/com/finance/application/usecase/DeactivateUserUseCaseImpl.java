package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.DeactivateUserUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeactivateUserUseCaseImpl implements DeactivateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeactivateUserUseCaseImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final CurrentUserPort currentUserPort;
    private final AuditPort auditPort;

    public DeactivateUserUseCaseImpl(UserRepositoryPort userRepositoryPort, CurrentUserPort currentUserPort,
                                     AuditPort auditPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.currentUserPort = currentUserPort;
        this.auditPort = auditPort;
    }

    @Override
    public User handle(String id) {
        log.info("Deactivating user id={}", id);
        if (!id.equals(currentUserPort.requireUserId())) {
            throw new ForbiddenException("Somente o próprio usuário pode desativar a conta");
        }
        User user = userRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
        Object oldValue = new UpdateUserUseCaseImpl.UserAuditSnapshot(user.getId(), user.getEmail(), user.getCpf(),
            user.isActive());
        user.deactivate();
        User saved = userRepositoryPort.save(user);
        auditPort.record(AppConstants.ENTITY_USER, saved.getId(), AuditAction.ATUALIZACAO, oldValue,
            new UpdateUserUseCaseImpl.UserAuditSnapshot(saved.getId(), saved.getEmail(), saved.getCpf(),
                saved.isActive()));
        log.info("User deactivated id={}", saved.getId());
        return saved;
    }
}
