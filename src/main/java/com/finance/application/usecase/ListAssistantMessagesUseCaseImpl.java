package com.finance.application.usecase;

import com.finance.application.query.PageResult;
import com.finance.domain.model.AssistantMessage;
import com.finance.domain.ports.in.ListAssistantMessagesUseCase;
import com.finance.domain.ports.out.AssistantMessageRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ListAssistantMessagesUseCaseImpl implements ListAssistantMessagesUseCase {

    private final AssistantMessageRepositoryPort messageRepositoryPort;

    public ListAssistantMessagesUseCaseImpl(AssistantMessageRepositoryPort messageRepositoryPort) {
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public PageResult<AssistantMessage> handle(int page, int size) {
        return messageRepositoryPort.search(page, size);
    }
}
