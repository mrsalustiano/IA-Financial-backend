package com.finance.adapters.in.rest.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.domain.enums.AccountType;
import com.finance.support.AbstractApiIT;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncomeApiIT extends AbstractApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollections() {
        mongoTemplate.getDb().getCollection("accounts").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("incomes").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("audit_logs").deleteMany(new Document());
    }

    @Test
    void createIncomeWithValidAccountReturns201() throws Exception {
        String accountId = createAccount("Salary Account", AccountType.CONTA_CORRENTE);
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Salary",
                    "amount", 5000.75,
                    "receivedAt", "2026-10-05"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data.description").value("Salary"))
            .andExpect(jsonPath("$.data.amount").value(5000.75))
            .andExpect(jsonPath("$.data.accountId").value(accountId))
            .andExpect(jsonPath("$.data.receivedAt").value("2026-10-05"))
            .andExpect(jsonPath("$.data.userId").value("LOCAL_USER"))
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void createIncomeWithZeroAmountReturns400() throws Exception {
        String accountId = createAccount("Zero", AccountType.CONTA_CORRENTE);
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Zero income",
                    "amount", 0,
                    "receivedAt", "2026-10-05"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void createIncomeWithNegativeAmountReturns400() throws Exception {
        String accountId = createAccount("Negative", AccountType.CONTA_CORRENTE);
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Negative income",
                    "amount", -10.5,
                    "receivedAt", "2026-10-05"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createIncomeWithUnknownAccountReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", UUID.randomUUID().toString(),
                    "description", "Orphan income",
                    "amount", 100,
                    "receivedAt", "2026-10-05"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Conta não encontrada")));
    }

    @Test
    void createIncomeOnInactiveAccountReturns422() throws Exception {
        String accountId = createAccount("Inactive Account", AccountType.CONTA_CORRENTE);
        deactivateAccount(accountId);
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Blocked income",
                    "amount", 100,
                    "receivedAt", "2026-10-05"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
            .andExpect(jsonPath("$.message").value("Conta inativa: Inactive Account"));
    }

    @Test
    void createIncomeWithFutureDateReturns400() throws Exception {
        String accountId = createAccount("Future", AccountType.CONTA_CORRENTE);
        mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Future income",
                    "amount", 100,
                    "receivedAt", "2099-01-01"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Erro de validação"));
    }

    @Test
    void updateIncomeChangesFieldsAndRevalidatesAccount() throws Exception {
        String accountA = createAccount("Account A", AccountType.CONTA_CORRENTE);
        String accountB = createAccount("Account B", AccountType.CONTA_CORRENTE);
        String incomeId = createIncome(accountA, "Bonus", 1200.50, "2026-09-30");

        mockMvc.perform(put("/api/v1/incomes/{id}", incomeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountB,
                    "description", "Updated bonus",
                    "amount", 1500.00,
                    "receivedAt", "2026-10-01"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accountId").value(accountB))
            .andExpect(jsonPath("$.data.description").value("Updated bonus"))
            .andExpect(jsonPath("$.data.amount").value(1500.00))
            .andExpect(jsonPath("$.data.receivedAt").value("2026-10-01"));
    }

    @Test
    void updateIncomeWithZeroAmountReturns400() throws Exception {
        String accountId = createAccount("Account Z", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "Old", 10, "2026-09-30");
        mockMvc.perform(put("/api/v1/incomes/{id}", incomeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Bad",
                    "amount", 0,
                    "receivedAt", "2026-09-30"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteIncomeSoftDeletes() throws Exception {
        String accountId = createAccount("Account D", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "To delete", 99.90, "2026-09-30");
        mockMvc.perform(delete("/api/v1/incomes/{id}", incomeId))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/incomes/{id}", incomeId))
            .andExpect(status().isNotFound());
        Document stored = mongoTemplate.findById(incomeId, Document.class, "incomes");
        assertThat(stored).isNotNull();
        assertThat(stored.getBoolean("deleted")).isTrue();
    }

    @Test
    void getUnknownIncomeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/incomes/{id}", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Receita não encontrada")));
    }

    @Test
    void listIncomesByAccountAndDateRange() throws Exception {
        String accountA = createAccount("Range A", AccountType.CONTA_CORRENTE);
        String accountB = createAccount("Range B", AccountType.CONTA_CORRENTE);
        createIncome(accountA, "September salary", 3000, "2026-09-05");
        createIncome(accountA, "October salary", 3000, "2026-10-05");
        createIncome(accountB, "Other account", 500, "2026-09-10");

        mockMvc.perform(get("/api/v1/incomes")
                .param("accountId", accountA)
                .param("startDate", "2026-09-01")
                .param("endDate", "2026-09-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].description").value("September salary"));

        mockMvc.perform(get("/api/v1/incomes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(3));

        mockMvc.perform(get("/api/v1/incomes").param("size", "2").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void listIncomesSortedByReceivedAtAscendingWhenRequested() throws Exception {
        String accountId = createAccount("Sort account", AccountType.DINHEIRO);
        createIncome(accountId, "Older income", 10, "2026-09-01");
        createIncome(accountId, "Newer income", 20, "2026-10-01");

        mockMvc.perform(get("/api/v1/incomes")
                .param("accountId", accountId)
                .param("sort", "receivedAt,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content[0].description").value("Older income"))
            .andExpect(jsonPath("$.data.content[1].description").value("Newer income"));
    }

    @Test
    void createIncomeWritesAuditLog() throws Exception {
        String accountId = createAccount("Audit income", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "Audited income", 42, "2026-10-10");
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Income").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("entityId")).isEqualTo(incomeId);
        assertThat(auditLogs.getFirst().getString("newValue")).contains("Audited income");
    }

    @Test
    void updateIncomeWritesAuditLogWithOldAndNewValues() throws Exception {
        String accountId = createAccount("Audit update", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "Before income", 10, "2026-09-30");
        mockMvc.perform(put("/api/v1/incomes/{id}", incomeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "After income",
                    "amount", 20,
                    "receivedAt", "2026-09-30"))))
            .andExpect(status().isOk());
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Income").append("action", "ATUALIZACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("entityId")).isEqualTo(incomeId);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("Before income");
        assertThat(auditLogs.getFirst().getString("newValue")).contains("After income");
    }

    @Test
    void updateIncomeRefreshesUpdatedAtAndKeepsCreatedAt() throws Exception {
        String accountId = createAccount("Stamp income", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "Stamped", 10, "2026-09-30");
        Document before = mongoTemplate.findById(incomeId, Document.class, "incomes");
        Thread.sleep(5);
        mockMvc.perform(put("/api/v1/incomes/{id}", incomeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", "Stamped again",
                    "amount", 15,
                    "receivedAt", "2026-09-30"))))
            .andExpect(status().isOk());
        Document after = mongoTemplate.findById(incomeId, Document.class, "incomes");
        assertThat(after.getDate("updatedAt")).isAfter(before.getDate("updatedAt"));
        assertThat(after.getDate("createdAt")).isEqualTo(before.getDate("createdAt"));
    }

    @Test
    void deleteIncomeWritesAuditLogWithOldValue() throws Exception {
        String accountId = createAccount("Audit delete income", AccountType.DINHEIRO);
        String incomeId = createIncome(accountId, "Doomed income", 5, "2026-09-30");
        mockMvc.perform(delete("/api/v1/incomes/{id}", incomeId))
            .andExpect(status().isNoContent());
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Income").append("action", "EXCLUSAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("Doomed income");
        assertThat(auditLogs.getFirst().getString("newValue")).isNull();
    }

    private String createAccount(String name, AccountType type) throws Exception {
        String response = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name, "type", type.name()))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private void deactivateAccount(String id) throws Exception {
        JsonNode account = objectMapper.readTree(mockMvc
            .perform(get("/api/v1/accounts/{id}", id))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        String name = account.path("data").path("name").asText();
        String type = account.path("data").path("type").asText();
        mockMvc.perform(put("/api/v1/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", name, "description", "", "type", type, "active", false))))
            .andExpect(status().isOk());
    }

    private String createIncome(String accountId, String description, double amount, String receivedAt) throws Exception {
        String response = mockMvc.perform(post("/api/v1/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accountId", accountId,
                    "description", description,
                    "amount", amount,
                    "receivedAt", receivedAt))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
