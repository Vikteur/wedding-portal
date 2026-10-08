package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FreshDatabaseIT {

    @Inject
    DataSource dataSource;

    @Test
    void dev_services_database_was_started_by_this_run_and_holds_no_row_of_an_earlier_one() throws SQLException {
        // Given the application started against Dev Services PostgreSQL
        try (Connection c = dataSource.getConnection()) {

            // When the server's start is compared with this JVM's
            FreshDatabase.assertStartedAfterThisJvm(c);

            // Then the history holds only the V file rows, each installed by this server
            try (Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("select count(*), count(*) filter (where installed_on >= pg_postmaster_start_time())"
                            + " from flyway_schema_history")) {
                rs.next();
                assertThat(rs.getInt(1)).as("rows of the V files").isEqualTo(1);
                assertThat(rs.getInt(2)).as("rows installed by this server").isEqualTo(rs.getInt(1));
            }
        }
    }

    @Test
    void dev_services_runs_the_postgres_17_alpine_image() throws SQLException {
        // Given the application started against Dev Services PostgreSQL
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select version()")) {

            // When / Then
            rs.next();
            assertThat(rs.getString(1)).startsWith("PostgreSQL 17").contains("musl");
        }
    }
}
