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

class FinancialPlanApiIT extends AbstractApiIT {

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
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("juntar 10 mil"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"model":"llama3","response":"Meta viavel.\\n- Guardar 800 por mes\\n- Cortar delivery","done":true}
                        """)));
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                    com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/generate"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("meta impossivel"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500)));
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void cleanCollections() {
        registerAndLogin();
        for (String collection : List.of("accounts", "incomes", "financial_plans", "audit_logs")) {
            mongoTemplate.getDb().getCollection(collection).deleteMany(new Document());
        }
    }

    @Test
    void createsPlanWithAiHistoryAuditAndEvent() throws Exception {
        createAccountWithIncome("Salario plano", 3000);

        String body = api(post("/api/v1/financial-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goal":"juntar 10 mil","targetAmount":10000,"targetDate":"2026-12-31"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.goal").value("juntar 10 mil"))
            .andExpect(jsonPath("$.data.recommendations").isArray())
            .andExpect(jsonPath("$.data.provider").value("ollama"))
            .andReturn().getResponse().getContentAsString();

        String planId = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        Document history = mongoTemplate.findById(planId, Document.class, "financial_plans");
        assertThat(history).isNotNull();
        assertThat(history.getString("prompt")).contains("Meta do usuario: juntar 10 mil");
        assertThat(history.getString("response")).contains("Guardar 800 por mes");
        assertThat(history.get("recommendations", List.class)).isNotEmpty();

        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "FinancialPlan").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);

        api(get("/api/v1/financial-plans/{id}", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(planId));

        api(get("/api/v1/financial-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createPlanWithoutIncomesReturns400() throws Exception {
        api(post("/api/v1/financial-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goal":"meta sem dados"}
                        """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPlanWithOllamaErrorReturns503() throws Exception {
        createAccountWithIncome("Salario 503", 1000);
        api(post("/api/v1/financial-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goal":"meta impossivel"}
                        """))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void createPlanWithBlankGoalReturns400() throws Exception {
        createAccountWithIncome("Salario blank", 1000);
        api(post("/api/v1/financial-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goal":""}
                        """))
            .andExpect(status().isBadRequest());
    }

    private void createAccountWithIncome(String description, double amount) throws Exception {
        String account = api(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Conta plano %s","description":"","type":"CONTA_CORRENTE","active":true}
                    """.formatted(description)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String accountId = account.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        api(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"%s","amount":%s,"receivedAt":"%s"}
                        """.formatted(accountId, description, amount, java.time.LocalDate.now())))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions api(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
        throws Exception {
        return mockMvc.perform(auth(builder));
    }
}