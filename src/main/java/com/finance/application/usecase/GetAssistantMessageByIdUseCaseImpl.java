package com.finance.application.usecase;

import com.finance.domain.model.AssistantMessage;
import com.finance.domain.ports.in.GetAssistantMessageByIdUseCase;
import com.finance.domain.ports.out.AssistantMessageRepositoryPort;
import com.finance.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetAssistantMessageByIdUseCaseImpl implements GetAssistantMessageByIdUseCase {

    private final AssistantMessageRepositoryPort messageRepositoryPort;

    public GetAssistantMessageByIdUseCaseImpl(AssistantMessageRepositoryPort messageRepositoryPort) {
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public AssistantMessage handle(String id) {
        return messageRepositoryPort.findById(id)
            .orElseThrow(() -> new NotFoundException("Mensagem não encontrada: " + id));
    }
}
