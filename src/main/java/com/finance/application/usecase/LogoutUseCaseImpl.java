package com.finance.application.usecase;

import com.finance.domain.enums.AuditAction;
import com.finance.domain.model.RefreshToken;
import com.finance.domain.ports.in.LogoutUseCase;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import com.finance.shared.constants.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutUseCaseImpl.class);

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final AuditPort auditPort;

    public LogoutUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepositoryPort, AuditPort auditPort) {
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public void handle(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        refreshTokenRepositoryPort.findByTokenHash(AuthenticateUseCaseImpl.sha256(refreshToken))
            .filter(token -> !token.isRevoked())
            .ifPresent(token -> {
                Object oldValue = auditPort.snapshot(token);
                token.revoke(now);
                refreshTokenRepositoryPort.save(token);
                auditPort.record(AppConstants.ENTITY_REFRESH_TOKEN, token.getId(), AuditAction.ATUALIZACAO,
                    oldValue, token);
                log.info("Refresh token revoked userId={}", token.getUserId());
            });
    }
}
