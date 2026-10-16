package com.finance.application.usecase;

import com.finance.domain.model.User;
import com.finance.domain.ports.in.GetUserByIdUseCase;
import com.finance.domain.ports.out.UserRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetUserByIdUseCaseImpl implements GetUserByIdUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public GetUserByIdUseCaseImpl(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User handle(String id) {
        return userRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }
}
