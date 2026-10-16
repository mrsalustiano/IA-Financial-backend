package com.finance.adapters.in.rest.v1;

import com.finance.support.AbstractApiIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DailyBalanceApiIT extends AbstractApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollections() {
        registerAndLogin();
        mongoTemplate.getDb().getCollection("accounts").deleteMany(new org.bson.Document());
        mongoTemplate.getDb().getCollection("incomes").deleteMany(new org.bson.Document());
    }

    @Test
    void calculatesAccumulatedDailyBalance() throws Exception {
        String accountId = createAccount();
        createIncome(accountId, "Salario", 1000, "2026-10-05");
        createIncome(accountId, "Freelance", 500, "2026-10-10");
        createIncome(accountId, "Bonus", 250, "2026-10-10");

        api(get("/api/v1/daily-balance")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].date").value("2026-10-05"))
            .andExpect(jsonPath("$.data[0].totalIncome").value(1000))
            .andExpect(jsonPath("$.data[0].accumulated").value(1000))
            .andExpect(jsonPath("$.data[1].date").value("2026-10-10"))
            .andExpect(jsonPath("$.data[1].totalIncome").value(750))
            .andExpect(jsonPath("$.data[1].accumulated").value(1750));
    }

    @Test
    void returnsEmptyListWithoutMovements() throws Exception {
        api(get("/api/v1/daily-balance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    private String createAccount() throws Exception {
        String body = api(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Conta saldo","description":"","type":"CONTA_CORRENTE","active":true}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private void createIncome(String accountId, String description, double amount, String receivedAt)
        throws Exception {
        api(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"%s","amount":%s,"receivedAt":"%s"}
                        """.formatted(accountId, description, amount, receivedAt)))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions api(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
        throws Exception {
        return mockMvc.perform(auth(builder));
    }
}