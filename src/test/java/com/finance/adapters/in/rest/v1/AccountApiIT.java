package com.finance.adapters.in.rest.v1;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountApiIT extends AbstractApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollections() {
        mongoTemplate.getDb().getCollection("accounts").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("audit_logs").deleteMany(new Document());
    }

    @Test
    void createAccountReturns201AndEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Main Checking","description":"Everyday account","type":"CONTA_CORRENTE"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.data.name").value("Main Checking"))
            .andExpect(jsonPath("$.data.type").value("CONTA_CORRENTE"))
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.userId").value("LOCAL_USER"))
            .andExpect(jsonPath("$.data.id").isNotEmpty())
            .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    void createAccountWithoutNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"  ","type":"DINHEIRO"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Erro de validação"))
            .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("name")))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void createAccountWithUnknownTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Wallet","type":"NOT_A_TYPE"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createAccountWithDuplicatedNameReturns409() throws Exception {
        createAccount("Investment Broker", AccountType.INVESTIMENTO);
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"investment broker","type":"INVESTIMENTO"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("Já existe uma conta com o nome: investment broker"));
    }

    @Test
    void getAccountByIdReturnsAccount() throws Exception {
        String id = createAccount("Savings", AccountType.POUPANCA);
        mockMvc.perform(get("/api/v1/accounts/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.name").value("Savings"));
    }

    @Test
    void getUnknownAccountReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Conta não encontrada")));
    }

    @Test
    void updateAccountChangesFields() throws Exception {
        String id = createAccount("Old Name", AccountType.CONTA_CORRENTE);
        mockMvc.perform(put("/api/v1/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"New Name","description":"updated","type":"POUPANCA","active":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("New Name"))
            .andExpect(jsonPath("$.data.description").value("updated"))
            .andExpect(jsonPath("$.data.type").value("POUPANCA"))
            .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void listAccountsFiltersByActiveFlag() throws Exception {
        createAccount("Active one", AccountType.CONTA_CORRENTE);
        String inactive = createAccount("Inactive one", AccountType.POUPANCA);
        mockMvc.perform(put("/api/v1/accounts/{id}", inactive)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Inactive one","description":"","type":"POUPANCA","active":false}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts").param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("Active one"));

        mockMvc.perform(get("/api/v1/accounts").param("active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("Inactive one"));
    }

    @Test
    void listAccountsSortsByNameDescWhenRequested() throws Exception {
        createAccount("Alpha", AccountType.DINHEIRO);
        createAccount("Beta", AccountType.DINHEIRO);
        mockMvc.perform(get("/api/v1/accounts").param("sort", "name,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content[0].name").value("Beta"))
            .andExpect(jsonPath("$.data.content[1].name").value("Alpha"));
    }

    @Test
    void updateAccountRefreshesUpdatedAtAndKeepsCreatedAt() throws Exception {
        String id = createAccount("Stamped", AccountType.CONTA_CORRENTE);
        Document before = mongoTemplate.findById(id, Document.class, "accounts");
        Thread.sleep(5);
        mockMvc.perform(put("/api/v1/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Stamped again","description":"","type":"CONTA_CORRENTE","active":true}
                    """))
            .andExpect(status().isOk());
        Document after = mongoTemplate.findById(id, Document.class, "accounts");
        assertThat(after.getDate("updatedAt")).isAfter(before.getDate("updatedAt"));
        assertThat(after.getDate("createdAt")).isEqualTo(before.getDate("createdAt"));
    }

    @Test
    void updateAccountToExistingNameReturns409() throws Exception {
        createAccount("First", AccountType.DINHEIRO);
        String second = createAccount("Second", AccountType.DINHEIRO);
        mockMvc.perform(put("/api/v1/accounts/{id}", second)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"First","description":"","type":"DINHEIRO","active":true}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void deleteAccountSoftDeletesAndHidesFromRead() throws Exception {
        String id = createAccount("To Delete", AccountType.CONTA_CORRENTE);
        mockMvc.perform(delete("/api/v1/accounts/{id}", id))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/accounts/{id}", id))
            .andExpect(status().isNotFound());
        Document stored = mongoTemplate.findById(id, Document.class, "accounts");
        assertThat(stored).isNotNull();
        assertThat(stored.getBoolean("deleted")).isTrue();
        assertThat(stored.getDate("deletedAt")).isNotNull();
    }

    @Test
    void deleteUnknownAccountReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/{id}", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listAccountsWithFilterPaginationAndSort() throws Exception {
        String checking = createAccount("Checking 1", AccountType.CONTA_CORRENTE);
        createAccount("Checking 2", AccountType.CONTA_CORRENTE);
        createAccount("Savings 1", AccountType.POUPANCA);

        mockMvc.perform(get("/api/v1/accounts")
                .param("type", "CONTA_CORRENTE")
                .param("size", "1")
                .param("page", "0")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(checking))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(1))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.content[0].name").value("Checking 1"));

        mockMvc.perform(get("/api/v1/accounts").param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(3));

        mockMvc.perform(get("/api/v1/accounts").param("type", "CARTAO_CREDITO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void createAccountWritesAuditLogWithCreateAction() throws Exception {
        String id = createAccount("Audited", AccountType.DINHEIRO);
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs").find().into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        Document audit = auditLogs.getFirst();
        assertThat(audit.getString("entityType")).isEqualTo("Account");
        assertThat(audit.getString("entityId")).isEqualTo(id);
        assertThat(audit.getString("action")).isEqualTo("CRIACAO");
        assertThat(audit.getString("oldValue")).isNull();
        assertThat(audit.getString("newValue")).contains("Audited");
        assertThat(audit.getDate("createdAt")).isNotNull();
    }

    @Test
    void updateAccountWritesAuditLogWithOldAndNewValues() throws Exception {
        String id = createAccount("Before", AccountType.DINHEIRO);
        mockMvc.perform(put("/api/v1/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"After","description":"","type":"DINHEIRO","active":true}
                    """))
            .andExpect(status().isOk());
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("action", "ATUALIZACAO")).into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("Before");
        assertThat(auditLogs.getFirst().getString("newValue")).contains("After");
    }

    @Test
    void deleteAccountWritesAuditLogWithOldValue() throws Exception {
        String id = createAccount("Doomed", AccountType.CONTA_CORRENTE);
        mockMvc.perform(delete("/api/v1/accounts/{id}", id))
            .andExpect(status().isNoContent());
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("action", "EXCLUSAO")).into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("Doomed");
        assertThat(auditLogs.getFirst().getString("newValue")).isNull();
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Corpo da requisição malformado"));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Recurso não encontrado"));
    }

    private String createAccount(String name, AccountType type) throws Exception {
        Map<String, Object> body = Map.of("name", name, "type", type.name());
        String response = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
