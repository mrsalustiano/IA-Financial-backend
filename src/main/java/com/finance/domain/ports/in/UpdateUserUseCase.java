package com.finance.domain.ports.in;

import com.finance.application.command.UpdateUserCommand;
import com.finance.domain.model.User;

public interface UpdateUserUseCase {

    User handle(String id, UpdateUserCommand command);
}
