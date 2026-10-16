package com.finance.domain.ports.in;

import com.finance.application.command.AskAssistantCommand;
import com.finance.domain.model.AssistantMessage;

public interface AskAssistantUseCase {

    AssistantMessage handle(AskAssistantCommand command);
}
