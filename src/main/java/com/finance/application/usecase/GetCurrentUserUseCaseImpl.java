package com.finance.application.usecase;

import com.finance.domain.model.User;
import com.finance.domain.ports.in.GetCurrentUserUseCase;
import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.ForbiddenException;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {

    private final CurrentUserPort currentUserPort;
    private final UserRepositoryPort userRepositoryPort;

    public GetCurrentUserUseCaseImpl(CurrentUserPort currentUserPort, UserRepositoryPort userRepositoryPort) {
        this.currentUserPort = currentUserPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User handle() {
        String userId = currentUserPort.requireUserId();
        User user = userRepositoryPort.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
        if (!user.isActive()) {
            throw new ForbiddenException("Usuário inativo");
        }
        return user;
    }
}
