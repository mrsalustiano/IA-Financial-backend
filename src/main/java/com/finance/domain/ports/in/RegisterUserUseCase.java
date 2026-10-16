package com.finance.domain.ports.in;

import com.finance.application.command.RegisterUserCommand;
import com.finance.domain.model.User;

public interface RegisterUserUseCase {

    User handle(RegisterUserCommand command);
}
