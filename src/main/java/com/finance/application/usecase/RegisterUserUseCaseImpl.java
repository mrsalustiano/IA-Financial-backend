package com.finance.application.usecase;

import com.finance.application.command.RegisterUserCommand;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.events.UserRegisteredEvent;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.RegisterUserUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.EventPublisherPort;
import com.finance.domain.ports.out.PasswordHasherPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.application.validator.UserValidator;
import com.finance.domain.validator.CpfValidator;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCaseImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final UserValidator userValidator;
    private final AuditPort auditPort;
    private final EventPublisherPort eventPublisherPort;

    public RegisterUserUseCaseImpl(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                   UserValidator userValidator, AuditPort auditPort,
                                   EventPublisherPort eventPublisherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.userValidator = userValidator;
        this.auditPort = auditPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public User handle(RegisterUserCommand command) {
        log.info("Registering user email={}", command.email());
        Instant now = Instant.now();
        String passwordHash = passwordHasherPort.hash(command.password());
        User user = User.create(Identifiers.newId(), command.email(), CpfValidator.sanitize(command.cpf()),
            passwordHash, command.requestedActive(), now);
        userValidator.ensureRegistrable(user, command.password());
        User saved = userRepositoryPort.save(user);
        auditPort.record(AppConstants.ENTITY_USER, saved.getId(), AuditAction.CRIACAO, null, snapshot(saved));
        eventPublisherPort.publish(new UserRegisteredEvent(saved.getId(), saved.getEmail(), now));
        log.info("User registered id={}", saved.getId());
        return saved;
    }

    public record UserSnapshot(String id, String email, String cpf, boolean active) {
    }

    private UserSnapshot snapshot(User user) {
        return new UserSnapshot(user.getId(), user.getEmail(), user.getCpf(), user.isActive());
    }
}
