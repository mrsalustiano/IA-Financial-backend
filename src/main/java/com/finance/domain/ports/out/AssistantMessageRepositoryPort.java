package com.finance.domain.ports.out;

import com.finance.application.query.PageResult;
import com.finance.domain.model.AssistantMessage;

import java.util.Optional;

public interface AssistantMessageRepositoryPort {

    AssistantMessage save(AssistantMessage message);

    Optional<AssistantMessage> findById(String id);

    PageResult<AssistantMessage> search(int page, int size);
}
