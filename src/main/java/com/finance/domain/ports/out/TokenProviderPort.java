package com.finance.domain.ports.out;

import com.finance.domain.model.AccessTokenClaims;

public interface TokenProviderPort {

    String createAccessToken(String userId, String email);

    AccessTokenClaims parseAccessToken(String token);

    long accessTokenTtlSeconds();
}
