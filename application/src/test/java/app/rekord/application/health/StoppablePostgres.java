package app.rekord.application.health;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import java.util.UUID;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A PostgreSQL 17 that the readiness test can stop while the application runs. */
public class StoppablePostgres implements QuarkusTestResourceLifecycleManager {

    static final String USER = "readiness_" + UUID.randomUUID().toString().substring(0, 8);
    static final String PASSWORD = UUID.randomUUID().toString();

    private static PostgreSQLContainer container;

    @Override
    public Map<String, String> start() {
        container = new PostgreSQLContainer("postgres:17-alpine").withUsername(USER).withPassword(PASSWORD);
        container.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", container.getJdbcUrl(),
                "quarkus.datasource.username", USER,
                "quarkus.datasource.password", PASSWORD);
    }

    /** Idempotent: the test stops the container early, and the framework calls this again at the end. */
    @Override
    public void stop() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    static void stopDatabase() {
        if (container == null) {
            throw new IllegalStateException("The database was not started: run the test with ReadinessTestProfile");
        }
        container.stop();
    }
}
