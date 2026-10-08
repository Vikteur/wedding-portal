package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SchemaMigrationIT {

    @Inject
    DataSource dataSource;

    @Inject
    Flyway flyway;

    @Test
    void flyway_applied_every_migration_once_in_order_and_successfully_starting_with_the_v1_baseline()
            throws SQLException {
        // Given: the application started against Dev Services PostgreSQL and the versioned migrations Flyway resolves
        List<String> resolved = Arrays.stream(flyway.info().all())
                .filter(i -> i.getVersion() != null)
                .map(i -> i.getVersion().getVersion())
                .toList();

        // When
        List<String> applied = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "select version, script, type, success from flyway_schema_history order by installed_rank")) {
            while (rs.next()) {
                // Then every row is a successful SQL migration
                assertThat(rs.getString("type")).isEqualTo("SQL");
                assertThat(rs.getBoolean("success")).isTrue();
                if (applied.isEmpty()) {
                    assertThat(rs.getString("script")).isEqualTo("db/migration/V1__baseline.sql");
                }
                applied.add(rs.getString("version"));
            }
        }

        // And the history is the resolved list, once each, in order, starting with V1
        assertThat(applied).isEqualTo(resolved).first().isEqualTo("1");
    }

    @Test
    void the_migrations_create_no_view_sequence_or_extension() throws SQLException {
        // Given: the application started against Dev Services PostgreSQL

        // When

        List<String> views = column(
                "select table_name from information_schema.views"
                        + " where table_schema not in ('pg_catalog', 'information_schema')");
        List<String> sequences = column("select sequence_name from information_schema.sequences");
        List<String> extensions = column("select extname from pg_extension");

        // Then
        assertThat(views).isEmpty();
        assertThat(sequences).isEmpty();
        assertThat(extensions).containsExactly("plpgsql");
    }

    @Test
    void dev_services_runs_postgres_17() throws SQLException {
        // Given: the application started against Dev Services PostgreSQL

        // When
        List<String> version = column("select current_setting('server_version_num')");

        // Then
        assertThat(version).hasSize(1);
        assertThat(version.get(0)).startsWith("17");
    }

    private List<String> column(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        return values;
    }
}
