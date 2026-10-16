package com.finance.domain.ports.in;

import com.finance.domain.model.User;

public interface GetUserByIdUseCase {

    User handle(String id);
}
