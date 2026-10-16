package com.finance.adapters.out.ollama;

import com.finance.domain.model.AiPrompt;
import com.finance.domain.model.AiResponse;
import com.finance.domain.ports.out.AiProviderPort;
import com.finance.shared.exception.ExternalServiceException;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OllamaAdapter implements AiProviderPort {

    public static final String PROVIDER_NAME = "ollama";

    private final OllamaClient ollamaClient;
    private final OllamaProperties properties;

    public OllamaAdapter(OllamaClient ollamaClient, OllamaProperties properties) {
        this.ollamaClient = ollamaClient;
        this.properties = properties;
    }

    @Override
    public AiResponse complete(AiPrompt prompt) {
        String fullPrompt = buildPrompt(prompt);
        OllamaResponse response = ollamaClient.generate(OllamaRequest.of(properties.getModel(), fullPrompt));
        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new ExternalServiceException("Ollama retornou resposta vazia");
        }
        String model = response.model() == null || response.model().isBlank()
            ? properties.getModel()
            : response.model();
        return new AiResponse(response.response(), PROVIDER_NAME, model);
    }

    private String buildPrompt(AiPrompt prompt) {
        if (prompt.context().isEmpty()) {
            return prompt.prompt();
        }
        String context = prompt.context().entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining("\n"));
        return prompt.prompt() + "\n\nContext:\n" + context;
    }
}
