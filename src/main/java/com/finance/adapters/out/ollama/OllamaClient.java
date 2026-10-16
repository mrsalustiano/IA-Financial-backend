package com.finance.adapters.out.ollama;

import com.finance.shared.exception.ExternalServiceException;
import com.finance.shared.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(OllamaProperties properties) {
        this.properties = properties;
        java.net.http.HttpClient jdkHttpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .proxy(java.net.http.HttpClient.Builder.NO_PROXY)
            .connectTimeout(properties.getTimeout())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);
        requestFactory.setReadTimeout(properties.getTimeout());
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    public OllamaResponse generate(OllamaRequest request) {
        log.debug("Calling Ollama model={} promptLength={}", request.model(),
            request.prompt() == null ? 0 : request.prompt().length());
        try {
            ResponseEntity<OllamaResponse> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(OllamaResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("Ollama retornou status de erro: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new IntegrationException("Falha ao chamar o Ollama", ex);
        }
    }

    public OllamaProperties getProperties() {
        return properties;
    }
}
