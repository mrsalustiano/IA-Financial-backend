package com.finance.domain.ports.in;

public interface LogoutUseCase {

    void handle(String refreshToken);
}
