package com.finance.domain.ports.in;

import com.finance.domain.model.User;

public interface DeactivateUserUseCase {

    User handle(String id);
}
