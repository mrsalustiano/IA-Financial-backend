package com.finance.adapters.out.ollama;

import com.finance.shared.exception.ExternalServiceException;
import com.finance.shared.exception.IntegrationException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaClientIT {

    private WireMockServer wireMockServer;
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        ollamaClient = clientFor(wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void generateParsesSuccessfulResponse() {
        wireMockServer.stubFor(post(urlEqualTo("/api/generate"))
            .withRequestBody(WireMock.matchingJsonPath("$.prompt", equalTo("Hello")))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"model\":\"llama3\",\"response\":\"Hello back\",\"done\":true,\"other\":123}")));

        OllamaResponse response = ollamaClient.generate(OllamaRequest.of("llama3", "Hello"));

        assertThat(response).isNotNull();
        assertThat(response.response()).isEqualTo("Hello back");
        assertThat(response.model()).isEqualTo("llama3");
        assertThat(response.done()).isTrue();
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/generate"))
            .withRequestBody(WireMock.matchingJsonPath("$.stream", equalTo("false"))));
    }

    @Test
    void generateThrowsExternalServiceExceptionOnHttpError() {
        wireMockServer.stubFor(post(urlEqualTo("/api/generate"))
            .willReturn(aResponse().withStatus(500).withBody("boom")));

        assertThatThrownBy(() -> ollamaClient.generate(OllamaRequest.of("llama3", "Hello")))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("500");
    }

    @Test
    void generateThrowsIntegrationExceptionOnConnectionFailure() {
        int closedPort = wireMockServer.port();
        wireMockServer.stop();
        OllamaClient brokenClient = clientFor(closedPort);

        assertThatThrownBy(() -> brokenClient.generate(OllamaRequest.of("llama3", "Hello")))
            .isInstanceOf(IntegrationException.class)
            .hasMessageContaining("Falha ao chamar o Ollama");
    }

    private OllamaClient clientFor(int port) {
        OllamaProperties properties = new OllamaProperties();
        properties.setBaseUrl("http://localhost:" + port);
        properties.setModel("llama3");
        properties.setTimeout(Duration.ofSeconds(5));
        return new OllamaClient(properties);
    }
}
