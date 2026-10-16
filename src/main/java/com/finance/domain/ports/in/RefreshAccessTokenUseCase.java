package com.finance.domain.ports.in;

import com.finance.domain.model.AuthSession;

public interface RefreshAccessTokenUseCase {

    AuthSession handle(String refreshToken);
}
