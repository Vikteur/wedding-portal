package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JdbcTimeZoneIT {

    @Inject
    DataSource dataSource;

    @Test
    void the_jdbc_session_runs_in_utc() throws SQLException {
        // Given: a pooled connection

        // When
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("show timezone")) {

            // Then
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("UTC");
        }
    }

    @Test
    void a_timestamp_written_and_read_back_through_jdbc_is_in_utc() throws SQLException {
        // Given: an instant written with a +02:00 offset
        OffsetDateTime written = OffsetDateTime.parse("2026-06-01T12:00:00+02:00");

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (Statement s = c.createStatement()) {
                    s.execute("create temp table t (at timestamptz) on commit drop");
                }
                try (PreparedStatement insert = c.prepareStatement("insert into t (at) values (?)")) {
                    insert.setObject(1, written);
                    insert.executeUpdate();
                }

                // When
                OffsetDateTime read;
                String text;
                try (Statement s = c.createStatement();
                        ResultSet rs = s.executeQuery("select at, at::text from t")) {
                    assertThat(rs.next()).isTrue();
                    read = rs.getObject(1, OffsetDateTime.class);
                    text = rs.getString(2);
                }

                // Then
                assertThat(read.getOffset()).isEqualTo(ZoneOffset.UTC);
                assertThat(read.getHour()).isEqualTo(10);
                assertThat(read.toInstant()).isEqualTo(written.toInstant());
                assertThat(text).endsWith("+00");
            } finally {
                c.rollback();
            }
        }
    }

    @Test
    void hibernate_jdbc_time_zone_setting_is_utc() {
        // Given: the running application's configuration

        // When
        String value = ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.jdbc.timezone", String.class);

        // Then
        assertThat(value).isEqualTo("UTC");
    }
}
