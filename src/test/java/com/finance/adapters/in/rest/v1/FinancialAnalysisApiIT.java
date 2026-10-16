package com.finance.adapters.in.rest.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.finance.support.AbstractApiIT;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;


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

class FinancialAnalysisApiIT extends AbstractApiIT {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void ollamaProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        registry.add("ollama.base-url", () -> wireMockServer.baseUrl());
        registry.add("ollama.model", () -> "llama3");
        registry.add("ollama.timeout", () -> "PT5S");
        com.github.tomakehurst.wiremock.client.WireMock.configureFor(wireMockServer.port());
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                    com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/generate"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock
                    .containing("economia mensal"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"model":"llama3","response":"Saude financeira boa.\\n- Aumentar reserva\\n- Controlar gastos","done":true}
                        """)));
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                    com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/generate"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.equalTo("{}"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500)));
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void cleanCollections() {
        mongoTemplate.getDb().getCollection("accounts").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("incomes").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("financial_analyses").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("audit_logs").deleteMany(new Document());
    }

    @Test
    void generateAnalysisReturns201PersistsHistoryAuditAndEvent() throws Exception {
        createAccountWithIncome();

        String body = mockMvc.perform(post("/api/v1/financial-analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"focus":"economia mensal"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.score").isNumber())
            .andExpect(jsonPath("$.data.recommendations").isArray())
            .andExpect(jsonPath("$.data.provider").value("ollama"))
            .andReturn().getResponse().getContentAsString();

        String analysisId = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        Document history = mongoTemplate.findById(analysisId, Document.class, "financial_analyses");
        assertThat(history).isNotNull();
        assertThat(history.getString("prompt")).contains("Foco solicitado: economia mensal");
        assertThat(history.getString("response")).contains("Aumentar reserva");
        assertThat(history.get("recommendations", List.class)).isNotEmpty();
        assertThat(history.getLong("score")).isBetween(0L, 100L);

        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "FinancialAnalysis").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);

        mockMvc.perform(get("/api/v1/financial-analysis/{id}", analysisId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(analysisId));

        mockMvc.perform(get("/api/v1/financial-analysis")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void generateAnalysisWithoutIncomesReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/financial-analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Não há receitas suficientes para gerar uma análise"));
    }

    @Test
    void generateAnalysisWithOllamaDownReturns503() throws Exception {
        createAccountWithIncome();

        mockMvc.perform(post("/api/v1/financial-analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isServiceUnavailable());
    }

    private void createAccountWithIncome() throws Exception {
        String account = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Conta analise","description":"","type":"CONTA_CORRENTE","active":true}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String accountId = account.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"Salario","amount":3000,"receivedAt":"%s"}
                        """.formatted(accountId, java.time.LocalDate.now())))
            .andExpect(status().isCreated());
    }
}
