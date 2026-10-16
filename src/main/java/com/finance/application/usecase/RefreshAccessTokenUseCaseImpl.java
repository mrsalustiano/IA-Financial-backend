package com.finance.application.usecase;

import com.finance.domain.model.AuthSession;
import com.finance.domain.model.RefreshToken;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.RefreshAccessTokenUseCase;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.domain.ports.out.TokenProviderPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.UnauthorizedException;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RefreshAccessTokenUseCaseImpl implements RefreshAccessTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshAccessTokenUseCaseImpl.class);

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TokenProviderPort tokenProviderPort;
    private final Duration refreshTokenTtl;

    public RefreshAccessTokenUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                         UserRepositoryPort userRepositoryPort, TokenProviderPort tokenProviderPort,
                                         @Value("${security.jwt.refresh-token-ttl:PT168H}") Duration refreshTokenTtl) {
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.tokenProviderPort = tokenProviderPort;
        this.refreshTokenTtl = refreshTokenTtl != null ? refreshTokenTtl : Duration.ofDays(7);
    }

    @Override
    public AuthSession handle(String refreshToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepositoryPort.findByTokenHash(AuthenticateUseCaseImpl.sha256(refreshToken))
            .orElseThrow(() -> new UnauthorizedException("Refresh token inválido ou expirado"));
        if (!stored.isValid(now)) {
            log.warn("Rejected refresh token userId={} revoked={}", stored.getUserId(), stored.isRevoked());
            throw new UnauthorizedException("Refresh token inválido ou expirado");
        }
        User user = userRepositoryPort.findById(stored.getUserId())
            .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Usuário inativo");
        }
        // Rotacao: revoga o token atual e emite um novo par de tokens
        stored.revoke(now);
        refreshTokenRepositoryPort.save(stored);

        String accessToken = tokenProviderPort.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = Identifiers.newId();
        RefreshToken replacement = RefreshToken.create(Identifiers.newId(), user.getId(),
            AuthenticateUseCaseImpl.sha256(newRefreshToken), now.plus(refreshTokenTtl), now);
        refreshTokenRepositoryPort.save(replacement);
        log.info("Session refreshed userId={}", user.getId());
        return AuthSession.of(user, accessToken, newRefreshToken, tokenProviderPort.accessTokenTtlSeconds());
    }
}
