package com.finance.domain.ports.out;

import com.finance.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email, String excludeId);

    boolean existsByCpf(String cpf, String excludeId);

    List<String> findAllActiveUserIds();

    com.finance.application.query.PageResult<User> search(com.finance.application.query.UserSearchQuery query);
}
