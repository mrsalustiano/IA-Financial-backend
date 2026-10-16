package com.finance.adapters.in.rest.v1;

import com.finance.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIT extends AbstractApiIT {

    private static final String PASSWORD = "Senha@123";
    private String email;
    private String refreshToken;

    @org.junit.jupiter.api.BeforeEach
    void setUpAuth() {
        registerAndLogin();
        email = lastEmail;
        refreshToken = lastRefreshToken;
    }

    @Test
    void rejectsLoginWithWrongPassword() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"errada123\"}".formatted(email)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWithUnknownEmail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nao.existe@test.com\",\"password\":\"Senha@123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAuthenticatedUserOnMe() throws Exception {
        mockMvc.perform(auth(MockMvcRequestBuilders.get("/api/v1/auth/me")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void rejectsMeWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMeWithInvalidToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/me")
                .header("Authorization", "Bearer token-invalido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String newRefresh = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(body).at("/data/refreshToken").asText();

        // refresh token antigo não pode mais ser usado (rotação + revogação)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isUnauthorized());

        // e o novo segue válido
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(newRefresh)))
            .andExpect(status().isOk());
        refreshToken = newRefresh;
    }

    @Test
    void rejectsUnknownRefreshToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"nao-existe\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenIdempotently() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isNoContent());

        // idempotente: segunda chamada não falha
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isNoContent());

        // token revogado não renova mais
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/accounts"))
            .andExpect(status().isUnauthorized());
    }
}
