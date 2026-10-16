package com.finance.domain.ports.out;

import com.finance.domain.model.AiPrompt;
import com.finance.domain.model.AiResponse;

public interface AiProviderPort {

    AiResponse complete(AiPrompt prompt);
}
