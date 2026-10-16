package com.finance.application.validator;

import com.finance.domain.model.User;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.domain.validator.CpfValidator;
import com.finance.shared.exception.ConflictException;
import com.finance.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UserValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepositoryPort userRepositoryPort;

    public UserValidator(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public void ensureRegistrable(User user, String rawPassword) {
        ensureValidEmail(user.getEmail());
        ensureValidPassword(rawPassword);
        CpfValidator.ensureValid(user.getCpf());
        ensureEmailAvailable(user.getEmail(), null);
        ensureCpfAvailable(user.getCpf(), null);
    }

    public void ensureValidEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("E-mail inválido");
        }
    }

    public void ensureValidPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("A senha deve ter no mínimo " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }

    public void ensureEmailAvailable(String email, String excludeId) {
        if (userRepositoryPort.existsByEmail(email, excludeId)) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail");
        }
    }

    public void ensureCpfAvailable(String cpf, String excludeId) {
        if (userRepositoryPort.existsByCpf(cpf, excludeId)) {
            throw new ConflictException("Já existe um usuário cadastrado com este CPF");
        }
    }
}
