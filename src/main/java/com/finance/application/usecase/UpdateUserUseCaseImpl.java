package com.finance.application.usecase;

import com.finance.application.command.UpdateUserCommand;
import com.finance.application.validator.UserValidator;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.UpdateUserUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.domain.validator.CpfValidator;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateUserUseCaseImpl implements UpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserUseCaseImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final CurrentUserPort currentUserPort;
    private final UserValidator userValidator;
    private final AuditPort auditPort;

    public UpdateUserUseCaseImpl(UserRepositoryPort userRepositoryPort, CurrentUserPort currentUserPort,
                                 UserValidator userValidator, AuditPort auditPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.currentUserPort = currentUserPort;
        this.userValidator = userValidator;
        this.auditPort = auditPort;
    }

    @Override
    public User handle(String id, UpdateUserCommand command) {
        log.info("Updating user id={}", id);
        Instant now = Instant.now();
        ensureSelf(id);
        User user = userRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
        Object oldValue = snapshot(user);
        userValidator.ensureValidEmail(command.email());
        CpfValidator.ensureValid(command.cpf());
        userValidator.ensureEmailAvailable(command.email(), id);
        userValidator.ensureCpfAvailable(CpfValidator.sanitize(command.cpf()), id);
        user.updateProfile(command.email(), CpfValidator.sanitize(command.cpf()), now);
        User saved = userRepositoryPort.save(user);
        auditPort.record(AppConstants.ENTITY_USER, saved.getId(), AuditAction.ATUALIZACAO, oldValue, snapshot(saved));
        log.info("User updated id={}", saved.getId());
        return saved;
    }

    private void ensureSelf(String id) {
        if (!id.equals(currentUserPort.requireUserId())) {
            throw new ForbiddenException("Somente o próprio usuário pode alterar o cadastro");
        }
    }

    /**
     * Snapshot de auditoria SEM dados de senha (nunca registra passwordHash).
     */
    public record UserAuditSnapshot(String id, String email, String cpf, boolean active) {
    }

    private Object snapshot(User user) {
        return new UserAuditSnapshot(user.getId(), user.getEmail(), user.getCpf(), user.isActive());
    }
}
