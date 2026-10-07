package app.rekord.application.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A PostgreSQL 17 that a start-up test owns: it can prepare the database before Quarkus boots and look at it after
 * the boot failed, which Dev Services does not allow.
 */
final class StartupDatabase implements AutoCloseable {

    private final PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");

    StartupDatabase(String... sqlBeforeStartup) {
        container.start();
        for (String sql : sqlBeforeStartup) {
            execute(sql);
        }
    }

    String jdbcUrl() {
        return container.getJdbcUrl();
    }

    String username() {
        return container.getUsername();
    }

    String password() {
        return container.getPassword();
    }

    void execute(String sql) {
        try (Connection c = connect();
                Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    List<String> column(String sql) {
        List<String> values = new ArrayList<>();
        try (Connection c = connect();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return values;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), username(), password());
    }

    @Override
    public void close() {
        container.stop();
    }
}
