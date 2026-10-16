package com.finance.support;

import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.bson.Document;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractApiIT {

    private static final Object LOCK = new Object();
    private static volatile TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
    protected static String MONGO_HOST;
    protected static int MONGO_PORT;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String accessToken;
    protected String currentUserId;
    protected String lastEmail;
    protected String lastRefreshToken;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        synchronized (LOCK) {
            if (runningMongod == null) {
                runningMongod = Mongod.instance().start(Version.Main.V7_0);
                ServerAddress address = runningMongod.current().getServerAddress();
                MONGO_HOST = address.getHost();
                MONGO_PORT = address.getPort();
                createTestUser();
                Runtime.getRuntime().addShutdownHook(new Thread(runningMongod::close));
            }
        }
        registry.add("spring.data.mongodb.host", () -> MONGO_HOST);
        registry.add("spring.data.mongodb.port", () -> MONGO_PORT);
        registry.add("spring.data.mongodb.database", () -> "ia-financial-test");
    }

    private static void createTestUser() {
        try (var client = MongoClients.create("mongodb://" + MONGO_HOST + ":" + MONGO_PORT)) {
            client.getDatabase("admin").runCommand(new Document("createUser", "admin")
                .append("pwd", "admin123")
                .append("roles", List.of(new Document("role", "root").append("db", "admin"))));
        }
    }

    /**
     * Registra um usuário novo (e-mail/CPF únicos) e autentica, guardando accessToken e currentUserId.
     */
    protected void registerAndLogin() {
        String email = "it-" + java.util.UUID.randomUUID() + "@test.com";
        String password = "Senha@123";
        lastEmail = email;
        String body = """
            {"email":"%s","cpf":"%s","password":"%s"}
            """.formatted(email, com.finance.support.CpfTestSupport.randomValidCpf(), password);
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .status().isCreated());
            String loginResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(loginResponse);
            accessToken = json.at("/data/accessToken").asText();
            currentUserId = json.at("/data/user/id").asText();
            lastRefreshToken = json.at("/data/refreshToken").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao preparar usuário autenticado para o teste", ex);
        }
    }

    /** Requisição autenticada com o token do usuário criado em registerAndLogin(). */
    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + accessToken);
    }
}
