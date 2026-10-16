package com.finance.adapters.out.security;

import com.finance.config.JwtProperties;
import com.finance.domain.model.AccessTokenClaims;
import com.finance.domain.ports.out.TokenProviderPort;
import com.finance.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtTokenProviderAdapter(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(sha256KeyBytes(properties.getSecret()));
        this.issuer = properties.getIssuer();
        this.ttlSeconds = properties.getAccessTokenTtl().toSeconds();
    }

    @Override
    public String createAccessToken(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .signWith(key)
            .compact();
    }

    @Override
    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return new AccessTokenClaims(claims.getSubject(), claims.get("email", String.class),
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new UnauthorizedException("Token expirado");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Token inválido");
        }
    }

    @Override
    public long accessTokenTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * Deriva uma chave de 32 bytes (HS256) a partir do segredo configurado, garantindo o tamanho
     * minimo exigido independentemente do segredo informado.
     */
    private static byte[] sha256KeyBytes(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }
}
