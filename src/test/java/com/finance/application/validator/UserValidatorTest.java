package com.finance.application.validator;

import com.finance.domain.model.User;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ConflictException;
import com.finance.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private UserValidator validator;

    private User user(String email, String cpf) {
        return User.create("u-1", email, cpf, "hash", true, Instant.now());
    }

    @Test
    void acceptsValidRegistration() {
        when(userRepositoryPort.existsByEmail(anyString(), any())).thenReturn(false);
        when(userRepositoryPort.existsByCpf(anyString(), any())).thenReturn(false);

        assertThatCode(() -> validator.ensureRegistrable(user("a@test.com", "52998224725"), "Senha@123"))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> validator.ensureValidEmail("sem-arroba"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("E-mail");
        assertThatThrownBy(() -> validator.ensureValidEmail(null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> validator.ensureValidPassword("123"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("mínimo");
        assertThatThrownBy(() -> validator.ensureValidPassword(null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsInvalidCpf() {
        assertThatThrownBy(() -> validator.ensureRegistrable(user("a@test.com", "11111111111"), "Senha@123"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("CPF");
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(userRepositoryPort.existsByEmail("a@test.com", null)).thenReturn(true);

        assertThatThrownBy(() -> validator.ensureEmailAvailable("a@test.com", null))
            .isInstanceOf(ConflictException.class).hasMessageContaining("e-mail");
    }

    @Test
    void rejectsDuplicatedCpf() {
        when(userRepositoryPort.existsByCpf("52998224725", null)).thenReturn(true);

        assertThatThrownBy(() -> validator.ensureCpfAvailable("52998224725", null))
            .isInstanceOf(ConflictException.class).hasMessageContaining("CPF");
    }
}
