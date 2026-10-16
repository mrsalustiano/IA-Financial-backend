package com.finance.domain.ports.in;

import com.finance.domain.model.AuthSession;

public interface AuthenticateUseCase {

    AuthSession handle(String email, String password);
}
