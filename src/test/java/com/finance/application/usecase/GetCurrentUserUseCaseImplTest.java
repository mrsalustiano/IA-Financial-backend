package com.finance.application.usecase;

import com.finance.domain.model.User;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplTest {

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private GetCurrentUserUseCaseImpl useCase;

    @Test
    void returnsCurrentUser() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash", true, Instant.now());
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(user));

        assertThat(useCase.handle()).isSameAs(user);
    }

    @Test
    void rejectsInactiveUserAndMissingUser() {
        User inactive = User.create("u-1", "a@test.com", "52998224725", "hash", false, Instant.now());
        when(currentUserPort.requireUserId()).thenReturn("u-1");
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> useCase.handle()).isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("inativo");

        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.handle()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenUserMissing() {
        when(currentUserPort.requireUserId()).thenReturn("u-404");
        when(userRepositoryPort.findById("u-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle()).isInstanceOf(NotFoundException.class);
    }
}
