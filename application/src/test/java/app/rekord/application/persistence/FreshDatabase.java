package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Proves a PostgreSQL server was started by this run, so no row of an earlier run can be in it. The server uptime
 * is read on the server and the JVM uptime on the JVM: two durations on their own clocks, so a Docker VM clock
 * skew cannot fake the result.
 */
public final class FreshDatabase {

    private FreshDatabase() {}

    public static void assertStartedAfterThisJvm(Connection connection) throws SQLException {
        long serverUptimeMillis;
        try (Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery(
                        "select (extract(epoch from clock_timestamp() - pg_postmaster_start_time()) * 1000)::bigint")) {
            rs.next();
            serverUptimeMillis = rs.getLong(1);
        }
        long jvmUptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        assertThat(serverUptimeMillis)
                .as("the server must be younger than this JVM, so it was started by this run")
                .isLessThan(jvmUptimeMillis);
    }
}
