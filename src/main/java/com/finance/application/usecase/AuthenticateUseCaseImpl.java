package com.finance.application.usecase;

import com.finance.domain.model.AuthSession;
import com.finance.domain.model.RefreshToken;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.AuthenticateUseCase;
import com.finance.domain.ports.out.PasswordHasherPort;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.domain.ports.out.TokenProviderPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.UnauthorizedException;
import com.finance.shared.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateUseCaseImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;
    private final Duration refreshTokenTtl;

    public AuthenticateUseCaseImpl(UserRepositoryPort userRepositoryPort,
                                   RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                   PasswordHasherPort passwordHasherPort, TokenProviderPort tokenProviderPort,
                                   @Value("${security.jwt.refresh-token-ttl:PT168H}") Duration refreshTokenTtl) {
        this.userRepositoryPort = userRepositoryPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
        this.refreshTokenTtl = refreshTokenTtl != null ? refreshTokenTtl : Duration.ofDays(7);
    }

    @Override
    public AuthSession handle(String email, String password) {
        Instant now = Instant.now();
        User user = userRepositoryPort.findByEmail(normalize(email))
            .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!passwordHasherPort.matches(password, user.getPasswordHash())) {
            log.warn("Failed login attempt email={}", email);
            throw new UnauthorizedException("Credenciais inválidas");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("Usuário inativo");
        }
        return issueSession(user, now);
    }

    AuthSession issueSession(User user, Instant now) {
        String accessToken = tokenProviderPort.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = Identifiers.newId();
        RefreshToken stored = RefreshToken.create(Identifiers.newId(), user.getId(), sha256(refreshToken),
            now.plus(refreshTokenTtl), now);
        refreshTokenRepositoryPort.save(stored);
        log.info("Session issued userId={}", user.getId());
        return AuthSession.of(user, accessToken, refreshToken, tokenProviderPort.accessTokenTtlSeconds());
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }
}
