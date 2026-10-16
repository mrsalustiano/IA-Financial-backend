package com.finance.application.usecase;

import com.finance.application.query.PageResult;
import com.finance.application.query.UserSearchQuery;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.ListUsersUseCase;
import com.finance.domain.ports.out.UserRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ListUsersUseCaseImpl implements ListUsersUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepositoryPort userRepositoryPort;

    public ListUsersUseCaseImpl(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public PageResult<User> handle(Boolean active, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return userRepositoryPort.search(new UserSearchQuery(active,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
