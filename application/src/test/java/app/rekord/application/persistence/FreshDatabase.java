package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Proves a PostgreSQL server was started by this run. With no reused container and no volume mounted on its data
 * directory (none is configured), a server started by this run holds no row of an earlier run. The server uptime
 * is read on the server and the JVM uptime on the JVM: two durations on their own clocks, so a Docker VM clock
 * skew cannot fake the result.
 */
public final class FreshDatabase {

    private FreshDatabase() {}

    public static void assertStartedAfterThisJvm(Connection connection) throws SQLException {
        assertYounger(serverUptimeMillis(connection), ManagementFactory.getRuntimeMXBean().getUptime());
    }

    /** How long the server has been up, in milliseconds, measured by the server's own clock. */
    public static long serverUptimeMillis(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery(
                        "select (extract(epoch from clock_timestamp() - pg_postmaster_start_time()) * 1000)::bigint")) {
            assertThat(rs.next()).as("the server uptime query returns a row").isTrue();
            return rs.getLong(1);
        }
    }

    static void assertYounger(long serverUptimeMillis, long jvmUptimeMillis) {
        assertThat(serverUptimeMillis)
                .as("the server must be younger than this JVM, so it was started by this run")
                .isLessThan(jvmUptimeMillis);
    }
}
