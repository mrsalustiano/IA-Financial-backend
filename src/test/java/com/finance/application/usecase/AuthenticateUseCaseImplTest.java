package com.finance.application.usecase;

import com.finance.domain.model.AuthSession;
import com.finance.domain.model.User;
import com.finance.domain.ports.out.PasswordHasherPort;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.domain.ports.out.TokenProviderPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticateUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @InjectMocks
    private AuthenticateUseCaseImpl useCase;

    private User user() {
        return User.create("u-1", "a@test.com", "52998224725", "hash", true, Instant.now());
    }

    @Test
    void issuesAccessAndRefreshTokensOnValidLogin() {
        when(userRepositoryPort.findByEmail("a@test.com")).thenReturn(Optional.of(user()));
        when(passwordHasherPort.matches("Senha@123", "hash")).thenReturn(true);
        when(tokenProviderPort.createAccessToken("u-1", "a@test.com")).thenReturn("access-token");
        when(tokenProviderPort.accessTokenTtlSeconds()).thenReturn(900L);

        AuthSession session = useCase.handle(" A@Test.COM ", "Senha@123");

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.expiresInSeconds()).isEqualTo(900L);
        assertThat(session.user().getId()).isEqualTo("u-1");
        verify(refreshTokenRepositoryPort).save(any());
    }

    @Test
    void rejectsUnknownEmailAndWrongPasswordWithSameMessage() {
        when(userRepositoryPort.findByEmail("a@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.handle("a@test.com", "Senha@123"))
            .isInstanceOf(UnauthorizedException.class).hasMessageContaining("Credenciais inválidas");

        when(userRepositoryPort.findByEmail("a@test.com")).thenReturn(Optional.of(user()));
        when(passwordHasherPort.matches("errada", "hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.handle("a@test.com", "errada"))
            .isInstanceOf(UnauthorizedException.class).hasMessageContaining("Credenciais inválidas");
    }

    @Test
    void rejectsInactiveUser() {
        User inactive = user();
        inactive.deactivate();
        when(userRepositoryPort.findByEmail("a@test.com")).thenReturn(Optional.of(inactive));
        when(passwordHasherPort.matches("Senha@123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.handle("a@test.com", "Senha@123"))
            .isInstanceOf(ForbiddenException.class).hasMessageContaining("inativo");
    }

    @Test
    void storesOnlySha256HashOfRefreshToken() {
        when(userRepositoryPort.findByEmail("a@test.com")).thenReturn(Optional.of(user()));
        when(passwordHasherPort.matches("Senha@123", "hash")).thenReturn(true);
        when(tokenProviderPort.createAccessToken(any(), any())).thenReturn("access-token");
        when(tokenProviderPort.accessTokenTtlSeconds()).thenReturn(900L);

        AuthSession session = useCase.handle("a@test.com", "Senha@123");

        var stored = new com.finance.domain.model.RefreshToken[] {null};
        org.mockito.ArgumentCaptor<com.finance.domain.model.RefreshToken> captor =
            org.mockito.ArgumentCaptor.forClass(com.finance.domain.model.RefreshToken.class);
        verify(refreshTokenRepositoryPort).save(captor.capture());
        stored[0] = captor.getValue();
        assertThat(stored[0].getTokenHash()).isEqualTo(AuthenticateUseCaseImpl.sha256(session.refreshToken()));
        assertThat(stored[0].getTokenHash()).isNotEqualTo(session.refreshToken());
        assertThat(stored[0].getUserId()).isEqualTo("u-1");
        assertThat(stored[0].getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }
}
