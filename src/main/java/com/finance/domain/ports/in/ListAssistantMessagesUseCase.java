package com.finance.domain.ports.in;

import com.finance.application.query.PageResult;
import com.finance.domain.model.AssistantMessage;

public interface ListAssistantMessagesUseCase {

    PageResult<AssistantMessage> handle(int page, int size);
}
