package com.finance.adapters.out.ollama;

import com.finance.domain.model.AiPrompt;
import com.finance.domain.model.AiResponse;
import com.finance.shared.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaAdapterTest {

    @Mock
    private OllamaClient ollamaClient;

    @Captor
    private ArgumentCaptor<OllamaRequest> requestCaptor;

    @Test
    void completeReturnsAiResponse() {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel("llama3");
        when(ollamaClient.generate(any())).thenReturn(new OllamaResponse("llama3:latest", "The analysis", true));
        OllamaAdapter adapter = new OllamaAdapter(ollamaClient, properties);

        AiResponse response = adapter.complete(new AiPrompt("Analyze my spending", Map.of("month", "October")));

        assertThat(response.content()).isEqualTo("The analysis");
        assertThat(response.provider()).isEqualTo("ollama");
        assertThat(response.model()).isEqualTo("llama3:latest");
        verify(ollamaClient).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().model()).isEqualTo("llama3");
        assertThat(requestCaptor.getValue().prompt()).contains("Analyze my spending").contains("month: October");
        assertThat(requestCaptor.getValue().stream()).isFalse();
    }

    @Test
    void completeUsesConfiguredModelWhenResponseHasNoModel() {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel("mistral");
        when(ollamaClient.generate(any())).thenReturn(new OllamaResponse(null, "Result", true));
        OllamaAdapter adapter = new OllamaAdapter(ollamaClient, properties);

        AiResponse response = adapter.complete(new AiPrompt("Summarize", Map.of()));

        assertThat(response.model()).isEqualTo("mistral");
        verify(ollamaClient).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().prompt()).isEqualTo("Summarize");
    }

    @Test
    void completeThrowsWhenResponseIsEmpty() {
        OllamaProperties properties = new OllamaProperties();
        when(ollamaClient.generate(any())).thenReturn(new OllamaResponse("llama3", "  ", true));
        OllamaAdapter adapter = new OllamaAdapter(ollamaClient, properties);

        assertThatThrownBy(() -> adapter.complete(new AiPrompt("Question", null)))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("resposta vazia");
    }

    @Test
    void completeThrowsWhenResponseIsNull() {
        OllamaProperties properties = new OllamaProperties();
        when(ollamaClient.generate(any())).thenReturn(null);
        OllamaAdapter adapter = new OllamaAdapter(ollamaClient, properties);

        assertThatThrownBy(() -> adapter.complete(new AiPrompt("Question", Map.of())))
            .isInstanceOf(ExternalServiceException.class);
    }
}
