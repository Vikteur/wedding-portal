package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SchemaMigrationIT {

    @Inject
    DataSource dataSource;

    @Test
    void flyway_applied_v1_baseline_once_and_successfully() throws SQLException {
        // Given: the application started against Dev Services PostgreSQL

        // When
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "select version, script, type, success from flyway_schema_history order by installed_rank")) {

            // Then
            assertThat(rs.next()).as("one history row").isTrue();
            assertThat(rs.getString("version")).isEqualTo("1");
            assertThat(rs.getString("script")).isEqualTo("db/migration/V1__baseline.sql");
            assertThat(rs.getString("type")).isEqualTo("SQL");
            assertThat(rs.getBoolean("success")).isTrue();
            assertThat(rs.next()).as("no second row").isFalse();
        }
    }

    @Test
    void the_baseline_creates_no_table_view_sequence_or_extension() throws SQLException {
        // Given: the application started against Dev Services PostgreSQL

        // When
        List<String> tables = column(
                "select table_name from information_schema.tables"
                        + " where table_schema not in ('pg_catalog', 'information_schema') and table_type = 'BASE TABLE'");
        List<String> views = column(
                "select table_name from information_schema.views"
                        + " where table_schema not in ('pg_catalog', 'information_schema')");
        List<String> sequences = column("select sequence_name from information_schema.sequences");
        List<String> extensions = column("select extname from pg_extension");

        // Then
        assertThat(tables).containsExactly("flyway_schema_history");
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
