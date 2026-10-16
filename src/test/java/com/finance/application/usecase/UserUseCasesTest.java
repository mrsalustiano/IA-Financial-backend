package com.finance.application.usecase;

import com.finance.application.command.UpdateUserCommand;
import com.finance.application.query.PageResult;
import com.finance.application.query.UserSearchQuery;
import com.finance.application.validator.UserValidator;
import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.User;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserUseCasesTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private UserValidator userValidator;

    @Mock
    private AuditPort auditPort;

    @Test
    void updatesOwnProfileWithAuditWithoutPassword() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash-original", true, Instant.now());
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(user));
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserUseCaseImpl useCase = new UpdateUserUseCaseImpl(userRepositoryPort, currentUserPort,
            userValidator, auditPort);
        User updated = useCase.handle("u-1", new UpdateUserCommand("novo@test.com", "111.444.777-35"));

        assertThat(updated.getEmail()).isEqualTo("novo@test.com");
        assertThat(updated.getCpf()).isEqualTo("11144477735");
        assertThat(updated.getPasswordHash()).isEqualTo("hash-original");
        ArgumentCaptor<Object> newValue = ArgumentCaptor.forClass(Object.class);
        verify(auditPort).record(eq("User"), eq("u-1"), eq(AuditAction.ATUALIZACAO), any(), newValue.capture());
        assertThat(newValue.getValue()).isNotInstanceOf(User.class);
        assertThat(String.valueOf(newValue.getValue())).doesNotContain("hash-original").doesNotContain("passwordHash");
    }

    @Test
    void rejectsUpdateOfAnotherUserOrMissing() {
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        UpdateUserUseCaseImpl useCase = new UpdateUserUseCaseImpl(userRepositoryPort, currentUserPort,
            userValidator, auditPort);
        when(userRepositoryPort.findById("u-2")).thenReturn(Optional.of(User.create("u-2", "b@test.com",
            "11144477735", "hash", true, Instant.now())));
        assertThatThrownBy(() -> useCase.handle("u-2", new UpdateUserCommand("x@test.com", "11144477735")))
            .isInstanceOf(ForbiddenException.class);

        // 403 tem precedência sobre 404 (não vaza existência de ids alheios)
        assertThatThrownBy(() -> useCase.handle("u-404", new UpdateUserCommand("x@test.com", "11144477735")))
            .isInstanceOf(ForbiddenException.class);

        // dono do id, mas usuário inexistente no banco
        when(currentUserPort.requireUserId()).thenReturn("u-404");
        when(userRepositoryPort.findById("u-404")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.handle("u-404", new UpdateUserCommand("x@test.com", "11144477735")))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivatesOwnAccountWithAudit() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash", true, Instant.now());
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(user));
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeactivateUserUseCaseImpl useCase = new DeactivateUserUseCaseImpl(userRepositoryPort, currentUserPort,
            auditPort);
        User deactivated = useCase.handle("u-1");

        assertThat(deactivated.isActive()).isFalse();
        verify(auditPort).record(eq("User"), eq("u-1"), eq(AuditAction.ATUALIZACAO), any(), any());
    }

    @Test
    void rejectsDeactivationOfAnotherUser() {
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        when(userRepositoryPort.findById("u-2")).thenReturn(Optional.of(User.create("u-2", "b@test.com",
            "11144477735", "hash", true, Instant.now())));
        DeactivateUserUseCaseImpl useCase = new DeactivateUserUseCaseImpl(userRepositoryPort, currentUserPort,
            auditPort);

        assertThatThrownBy(() -> useCase.handle("u-2")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listsUsersPagedAndFiltered() {
        ListUsersUseCaseImpl useCase = new ListUsersUseCaseImpl(userRepositoryPort);
        when(userRepositoryPort.search(any(UserSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        PageResult<User> result = useCase.handle(true, 0, 20);

        assertThat(result).isNotNull();
        ArgumentCaptor<UserSearchQuery> captor = ArgumentCaptor.forClass(UserSearchQuery.class);
        verify(userRepositoryPort).search(captor.capture());
        assertThat(captor.getValue().active()).isTrue();
        assertThat(captor.getValue().pageable().getPageNumber()).isZero();
        assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void normalizesPaginationLimits() {
        ListUsersUseCaseImpl useCase = new ListUsersUseCaseImpl(userRepositoryPort);
        when(userRepositoryPort.search(any(UserSearchQuery.class)))
            .thenReturn(new PageResult<>(List.of(), 0, 100, 0, 0));

        useCase.handle(null, -5, 9999);

        ArgumentCaptor<UserSearchQuery> captor = ArgumentCaptor.forClass(UserSearchQuery.class);
        verify(userRepositoryPort).search(captor.capture());
        assertThat(captor.getValue().pageable().getPageNumber()).isZero();
        assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().active()).isNull();
    }
}
