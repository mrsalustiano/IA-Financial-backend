package com.finance.adapters.in.rest.v1;

import com.finance.support.AbstractApiIT;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyClosingApiIT extends AbstractApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private com.finance.scheduler.MonthlyClosingScheduler scheduler;

    @BeforeEach
    void cleanCollections() {
        registerAndLogin();
        for (String collection : List.of("accounts", "incomes", "monthly_closings", "audit_logs")) {
            mongoTemplate.getDb().getCollection(collection).deleteMany(new Document());
        }
    }

    @Test
    void generatesClosingSnapshotWithAuditAndEvent() throws Exception {
        YearMonth month = YearMonth.now();
        String accountId = createAccount();
        createIncome(accountId, 1200);
        createIncome(accountId, 800);

        api(post("/api/v1/monthly-closings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"%s"}
                        """.formatted(month)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.referenceMonth").value(month.toString()))
            .andExpect(jsonPath("$.data.totalIncome").value(2000))
            .andExpect(jsonPath("$.data.balance").value(2000));

        List<Document> snapshots = mongoTemplate.getDb().getCollection("monthly_closings")
            .find().into(new java.util.ArrayList<>());
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().getString("referenceMonth")).isEqualTo(month.toString());

        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "MonthlyClosing").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
    }

    @Test
    void regeneratesSameMonthUpdatesSnapshotWithAudit() throws Exception {
        YearMonth month = YearMonth.now();
        String accountId = createAccount();
        createIncome(accountId, 500);
        api(post("/api/v1/monthly-closings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"%s"}
                        """.formatted(month)))
            .andExpect(status().isCreated());
        createIncome(accountId, 700);
        api(post("/api/v1/monthly-closings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"%s"}
                        """.formatted(month)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.totalIncome").value(1200));

        List<Document> snapshots = mongoTemplate.getDb().getCollection("monthly_closings")
            .find().into(new java.util.ArrayList<>());
        assertThat(snapshots).hasSize(1);
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "MonthlyClosing").append("action", "ATUALIZACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("500");
        assertThat(auditLogs.getFirst().getString("newValue")).contains("1200");
    }

    @Test
    void schedulerGeneratesSnapshotForPreviousMonth() throws Exception {
        YearMonth previous = YearMonth.now().minusMonths(1);
        String accountId = createAccount();
        api(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"Antigo","amount":900,"receivedAt":"%s"}
                        """.formatted(accountId, previous.atDay(15))))
            .andExpect(status().isCreated());

        scheduler.closePreviousMonth();

        List<Document> snapshots = mongoTemplate.getDb().getCollection("monthly_closings")
            .find(new Document("referenceMonth", previous.toString()).append("userId", currentUserId))
            .into(new java.util.ArrayList<>());
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().get("totalIncome", org.bson.types.Decimal128.class).bigDecimalValue())
            .isEqualByComparingTo("900");
    }

    @Test
    void invalidMonthReturns400() throws Exception {
        api(post("/api/v1/monthly-closings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"13/2026"}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Mês inválido. Use o formato yyyy-MM"));
    }

    @Test
    void listsAndGetsClosings() throws Exception {
        YearMonth month = YearMonth.now();
        String accountId = createAccount();
        createIncome(accountId, 300);
        String body = api(post("/api/v1/monthly-closings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"%s"}
                        """.formatted(month)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        api(get("/api/v1/monthly-closings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));

        api(get("/api/v1/monthly-closings/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id));

        api(get("/api/v1/monthly-closings/{id}", "6f1f2b3c0000000000000000"))
            .andExpect(status().isNotFound());
    }

    private String createAccount() throws Exception {
        String body = api(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Conta fechamento","description":"","type":"CONTA_CORRENTE","active":true}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private void createIncome(String accountId, double amount) throws Exception {
        api(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountId":"%s","description":"Receita","amount":%s,"receivedAt":"%s"}
                        """.formatted(accountId, amount, YearMonth.now().atDay(10))))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions api(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
        throws Exception {
        return mockMvc.perform(auth(builder));
    }
}