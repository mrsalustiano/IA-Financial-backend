package com.finance.adapters.in.rest.v1;

import com.finance.support.AbstractApiIT;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantApiIT extends AbstractApiIT {

    private static final com.github.tomakehurst.wiremock.WireMockServer WIRE_MOCK =
        new com.github.tomakehurst.wiremock.WireMockServer(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void ollamaProperties(DynamicPropertyRegistry registry) {
        WIRE_MOCK.start();
        registry.add("ollama.base-url", () -> WIRE_MOCK.baseUrl());
        registry.add("ollama.model", () -> "llama3");
        registry.add("ollama.timeout", () -> "PT5S");
        com.github.tomakehurst.wiremock.client.WireMock.configureFor(WIRE_MOCK.port());
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                    com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/generate"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("qual meu saldo"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"model":"llama3","response":"Voce recebeu 3000 em outubro.","done":true}
                        """)));
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                    com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/generate"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("pergunta com erro"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(503)));
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void cleanCollections() {
        for (String collection : List.of("accounts", "incomes", "assistant_messages", "audit_logs")) {
            mongoTemplate.getDb().getCollection(collection).deleteMany(new Document());
        }
    }

    @Test
    void answersQuestionUsingRegisteredDataAndPersistsHistory() throws Exception {
        createAccountWithIncome(3000);

        String body = mockMvc.perform(post("/api/v1/assistant/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question":"qual meu saldo de outubro?"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.question").value("qual meu saldo de outubro?"))
            .andExpect(jsonPath("$.data.answer").value("Voce recebeu 3000 em outubro."))
            .andExpect(jsonPath("$.data.provider").value("ollama"))
            .andReturn().getResponse().getContentAsString();

        String messageId = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        Document history = mongoTemplate.findById(messageId, Document.class, "assistant_messages");
        assertThat(history).isNotNull();
        assertThat(history.getString("question")).isEqualTo("qual meu saldo de outubro?");
        assertThat(history.getString("answer")).contains("3000");

        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "AssistantMessage").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);

        mockMvc.perform(get("/api/v1/assistant/questions/{id}", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(messageId));

        mockMvc.perform(get("/api/v1/assistant/questions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void blankQuestionReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question":"  "}
                        """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void ollamaFailureReturns503() throws Exception {
        createAccountWithIncome(1000);
        mockMvc.perform(post("/api/v1/assistant/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question":"pergunta com erro"}
                        """))
            .andExpect(status().isServiceUnavailable());
    }

    private void createAccountWithIncome(double amount) throws Exception {
        String account = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Conta assistente","description":"","type":"CONTA_CORRENTE","active":true}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String accountId = account.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"Salario","amount":%s,"receivedAt":"%s"}
                        """.formatted(accountId, amount, java.time.LocalDate.now())))
            .andExpect(status().isCreated());
    }
}
