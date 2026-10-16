package com.finance.application.usecase;

import com.finance.domain.model.AuthSession;
import com.finance.domain.model.RefreshToken;
import com.finance.domain.model.User;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.domain.ports.out.TokenProviderPort;
import com.finance.domain.ports.out.UserRepositoryPort;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshAccessTokenUseCaseImplTest {

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @InjectMocks
    private RefreshAccessTokenUseCaseImpl useCase;

    private User user() {
        return User.create("u-1", "a@test.com", "52998224725", "hash", true, Instant.now());
    }

    private RefreshToken stored(String rawToken, Instant now) {
        return RefreshToken.create("rt-1", "u-1", AuthenticateUseCaseImpl.sha256(rawToken),
            now.plus(Duration.ofDays(7)), now);
    }

    @Test
    void rotatesTokensAndRevokesOldRefresh() {
        Instant now = Instant.now();
        String raw = "raw-refresh-token";
        when(refreshTokenRepositoryPort.findByTokenHash(AuthenticateUseCaseImpl.sha256(raw)))
            .thenReturn(Optional.of(stored(raw, now)));
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(user()));
        when(tokenProviderPort.createAccessToken("u-1", "a@test.com")).thenReturn("new-access");
        when(tokenProviderPort.accessTokenTtlSeconds()).thenReturn(900L);

        AuthSession session = useCase.handle(raw);

        assertThat(session.accessToken()).isEqualTo("new-access");
        assertThat(session.refreshToken()).isNotEqualTo(raw);
        verify(refreshTokenRepositoryPort, times(2)).save(any()); // revoke + replacement
    }

    @Test
    void rejectsMissingExpiredOrRevokedToken() {
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.handle("desconhecido"))
            .isInstanceOf(UnauthorizedException.class).hasMessageContaining("Refresh token");

        Instant now = Instant.now();
        RefreshToken expired = RefreshToken.rehydrate("rt-1", "u-1", "hash", now.minusSeconds(1), false, null,
            false, null, now, now);
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> useCase.handle("qualquer"))
            .isInstanceOf(UnauthorizedException.class);

        RefreshToken revoked = RefreshToken.rehydrate("rt-1", "u-1", "hash", now.plusSeconds(60), true, now,
            false, null, now, now);
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> useCase.handle("qualquer"))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsInactiveOrMissingUser() {
        Instant now = Instant.now();
        String raw = "raw-refresh-token";
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.of(stored(raw, now)));
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.handle(raw)).isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("não encontrado");

        User inactive = user();
        inactive.deactivate();
        when(userRepositoryPort.findById("u-1")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> useCase.handle(raw)).isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("inativo");
    }
}
