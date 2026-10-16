package com.finance.application.usecase;

import com.finance.domain.model.RefreshToken;
import com.finance.domain.ports.out.AuditPort;
import com.finance.domain.ports.out.RefreshTokenRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogoutUseCaseImplTest {

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private LogoutUseCaseImpl useCase;

    @Test
    void revokesActiveRefreshTokenWithAudit() {
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.create("rt-1", "u-1", AuthenticateUseCaseImpl.sha256("raw"),
            now.plusSeconds(600), now);
        when(refreshTokenRepositoryPort.findByTokenHash(AuthenticateUseCaseImpl.sha256("raw")))
            .thenReturn(Optional.of(token));
        when(auditPort.snapshot(any())).thenReturn("old");

        useCase.handle("raw");

        verify(refreshTokenRepositoryPort).save(any());
        verify(auditPort).record(org.mockito.ArgumentMatchers.eq("RefreshToken"), org.mockito.ArgumentMatchers.eq("rt-1"),
            org.mockito.ArgumentMatchers.eq(com.finance.domain.enums.AuditAction.ATUALIZACAO),
            org.mockito.ArgumentMatchers.eq("old"), any());
    }

    @Test
    void ignoresBlankUnknownOrAlreadyRevokedTokens() {
        useCase.handle(null);
        useCase.handle("  ");
        verify(refreshTokenRepositoryPort, never()).findByTokenHash(any());

        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());
        useCase.handle("desconhecido");
        verify(refreshTokenRepositoryPort, never()).save(any());

        Instant now = Instant.now();
        RefreshToken revoked = RefreshToken.rehydrate("rt-1", "u-1", "hash", now.plusSeconds(600), true, now,
            false, null, now, now);
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.of(revoked));
        useCase.handle("qualquer");
        verify(refreshTokenRepositoryPort, never()).save(any());
    }
}
