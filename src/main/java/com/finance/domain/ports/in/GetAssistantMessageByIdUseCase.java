package com.finance.domain.ports.in;

import com.finance.domain.model.AssistantMessage;

public interface GetAssistantMessageByIdUseCase {

    AssistantMessage handle(String id);
}
