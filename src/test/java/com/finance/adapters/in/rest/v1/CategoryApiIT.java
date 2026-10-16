package com.finance.adapters.in.rest.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.finance.support.AbstractApiIT;
import org.bson.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiIT extends AbstractApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollections() {
        registerAndLogin();
        mongoTemplate.getDb().getCollection("categories").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("audit_logs").deleteMany(new Document());
    }

    @Test
    void createCategoryReturns201WithEnvelopeAndAudit() throws Exception {
        String body = """
            {"name":"Mercado","description":"Supermercado","classification":"DESPESA","active":true}
            """;
        String location = api(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Mercado"))
            .andExpect(jsonPath("$.data.classification").value("DESPESA"))
            .andExpect(jsonPath("$.message").value("Success"))
            .andReturn().getResponse().getHeader("Location");

        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Category").append("action", "CRIACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("newValue")).contains("Mercado");
        assertThat(location).endsWith("/api/v1/categories/" + auditLogs.getFirst().getString("entityId"));
    }

    @Test
    void createCategoryWithMissingFieldsReturns400() throws Exception {
        api(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"sem nome"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Erro de validação"));
    }

    @Test
    void createCategoryWithUnknownClassificationReturns400() throws Exception {
        api(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"X","classification":"INVALIDA"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Corpo da requisição malformado"));
    }

    @Test
    void createDuplicateCategoryNameReturns409() throws Exception {
        createCategory("Duplicada", "DESPESA");
        api(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"duplicada","classification":"RECEITA"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void updateCategoryRefreshesUpdatedAtAndWritesAudit() throws Exception {
        String id = createCategory("ToUpdate", "DESPESA");
        Document before = mongoTemplate.findById(id, Document.class, "categories");
        Thread.sleep(5);
        api(put("/api/v1/categories/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Updated","description":"","classification":"RECEITA","active":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.classification").value("RECEITA"));

        Document after = mongoTemplate.findById(id, Document.class, "categories");
        assertThat(after.getDate("updatedAt")).isAfter(before.getDate("updatedAt"));
        assertThat(after.getDate("createdAt")).isEqualTo(before.getDate("createdAt"));
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Category").append("action", "ATUALIZACAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("ToUpdate");
        assertThat(auditLogs.getFirst().getString("newValue")).contains("Updated");
    }

    @Test
    void deleteCategorySoftDeletesHidesAndAudits() throws Exception {
        String id = createCategory("Doomed", "RECEITA");
        api(delete("/api/v1/categories/{id}", id))
            .andExpect(status().isNoContent());
        api(get("/api/v1/categories/{id}", id))
            .andExpect(status().isNotFound());
        Document document = mongoTemplate.findById(id, Document.class, "categories");
        assertThat(document).isNotNull();
        assertThat(document.getBoolean("deleted")).isTrue();
        assertThat(document.getDate("deletedAt")).isNotNull();
        List<Document> auditLogs = mongoTemplate.getDb().getCollection("audit_logs")
            .find(new Document("entityType", "Category").append("action", "EXCLUSAO"))
            .into(new java.util.ArrayList<>());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getString("oldValue")).contains("Doomed");
    }

    @Test
    void listCategoriesFiltersByClassificationAndSupportsPagination() throws Exception {
        createCategory("Cat A", "DESPESA");
        createCategory("Cat B", "RECEITA");
        createCategory("Cat C", "DESPESA");
        api(get("/api/v1/categories")
                .param("classification", "DESPESA")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.content[0].name").value("Cat A"));
    }

    private String createCategory(String name, String classification) throws Exception {
        return api(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","description":"","classification":"%s","active":true}
                        """.formatted(name, classification)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private org.springframework.test.web.servlet.ResultActions api(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
        throws Exception {
        return mockMvc.perform(auth(builder));
    }
}