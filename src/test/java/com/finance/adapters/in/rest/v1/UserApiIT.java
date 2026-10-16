package com.finance.adapters.in.rest.v1;

import com.finance.support.AbstractApiIT;
import com.finance.support.CpfTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIT extends AbstractApiIT {

    @org.junit.jupiter.api.BeforeEach
    void setUpAuthenticatedUser() {
        registerAndLogin();
    }

    @Test
    void registersUserAndNeverExposesPasswordHash() throws Exception {
        String cpf = CpfTestSupport.randomValidCpf();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"novo-user@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
                    .formatted(cpf)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("novo-user@test.com"))
            .andExpect(jsonPath("$.data.cpf").value(cpf))
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void registersInactiveUserWhenActiveFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(("{\"email\":\"inativo@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\","
                    + "\"active\":false}").formatted(CpfTestSupport.randomValidCpf())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String body = "{\"email\":\"duplicado@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
            .formatted(CpfTestSupport.randomValidCpf());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated());
        String anotherCpf = CpfTestSupport.randomValidCpf();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"duplicado@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
                    .formatted(anotherCpf)))
            .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateCpf() throws Exception {
        String cpf = CpfTestSupport.randomValidCpf();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cpf-um@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
                    .formatted(cpf)))
            .andExpect(status().isCreated());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cpf-dois@test.com\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
                    .formatted(cpf)))
            .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidCpf() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cpf-invalido@test.com\",\"cpf\":\"11111111111\","
                    + "\"password\":\"Senha@123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"senha-curta@test.com\",\"cpf\":\"%s\",\"password\":\"123\"}"
                    .formatted(CpfTestSupport.randomValidCpf())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sem-arroba\",\"cpf\":\"%s\",\"password\":\"Senha@123\"}"
                    .formatted(CpfTestSupport.randomValidCpf())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        String email = "inativo-login@test.com";
        String cpf = CpfTestSupport.randomValidCpf();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(("{\"email\":\"%s\",\"cpf\":\"%s\",\"password\":\"Senha@123\",\"active\":false}")
                    .formatted(email, cpf)))
            .andExpect(status().isCreated());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Senha@123\"}".formatted(email)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updatesOwnProfileAndAuditsWithoutPassword() throws Exception {
        String newEmail = "editado-" + java.util.UUID.randomUUID() + "@test.com";
        String newCpf = CpfTestSupport.randomValidCpf();
        mockMvc.perform(auth(MockMvcRequestBuilders.put("/api/v1/users/{id}", currentUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"cpf\":\"%s\"}".formatted(newEmail, newCpf)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(newEmail))
            .andExpect(jsonPath("$.data.cpf").value(newCpf))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        // não pode editar outro usuário
        String otherId = java.util.UUID.randomUUID().toString();
        mockMvc.perform(auth(MockMvcRequestBuilders.put("/api/v1/users/{id}", otherId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"outro@test.com\",\"cpf\":\"%s\"}".formatted(CpfTestSupport.randomValidCpf())))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsUsersPaged() throws Exception {
        mockMvc.perform(auth(MockMvcRequestBuilders.get("/api/v1/users"))
                .param("page", "0").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void deactivatesSelfAndBlocksLogin() throws Exception {
        mockMvc.perform(auth(MockMvcRequestBuilders.post("/api/v1/users/{id}/deactivate", currentUserId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.active").value(false));

        // usuário desativado não consegue mais logar
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Senha@123\"}".formatted(lastEmail)))
            .andExpect(status().isForbidden());

        // token antigo ainda é JWT válido, mas /me bloqueia usuário inativo
        mockMvc.perform(auth(MockMvcRequestBuilders.get("/api/v1/auth/me")))
            .andExpect(status().isForbidden());
    }
}
