package app.rekord.application.persistence.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A new, empty PostgreSQL 17 alpine container of this run (never reused) that Flyway can migrate step by step. */
public final class MigrationDatabase implements AutoCloseable {

    private static final String PRODUCTION_LOCATION = "classpath:db/migration";

    private final PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");

    public MigrationDatabase() {
        container.start();
    }

    public PostgreSQLContainer container() {
        return container;
    }

    public void migrateTo(String version) {
        migrateTo(version, PRODUCTION_LOCATION);
    }

    public void migrateTo(String version, String location) {
        Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .locations(location)
                .baselineOnMigrate(false)
                .target(version)
                .load()
                .migrate();
    }

    public Connection connection() throws SQLException {
        return container.createConnection("");
    }

    public SchemaSnapshot snapshot() throws SQLException {
        try (Connection c = connection()) {
            return SchemaSnapshot.read(c);
        }
    }

    /** The versions in the history, in installation order, each of which must have succeeded. */
    public List<String> historyVersions() throws SQLException {
        List<String> versions = new ArrayList<>();
        try (Connection c = connection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "select version, success from flyway_schema_history where version is not null order by installed_rank")) {
            while (rs.next()) {
                if (!rs.getBoolean("success")) {
                    throw new IllegalStateException("migration " + rs.getString("version") + " failed");
                }
                versions.add(rs.getString("version"));
            }
        }
        return versions;
    }

    @Override
    public void close() {
        container.stop();
    }
}
