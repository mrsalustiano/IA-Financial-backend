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

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractApiIT {

    private static final Object LOCK = new Object();
    private static volatile TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
    protected static String MONGO_HOST;
    protected static int MONGO_PORT;

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
}
