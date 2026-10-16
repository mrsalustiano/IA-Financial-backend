package com.finance.domain.ports.in;

import com.finance.application.query.PageResult;
import com.finance.domain.model.User;

public interface ListUsersUseCase {

    PageResult<User> handle(Boolean active, int page, int size);
}
