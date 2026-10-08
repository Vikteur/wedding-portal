package app.rekord.application.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import org.flywaydb.core.Flyway;

/**
 * A PostgreSQL that is migrated with the Flyway API before Quarkus boots, plus the rows a test names. The start-up
 * tests of the first-admin bootstrap use it: they look at the tables after the application started.
 *
 * <p>When the application starts, Quarkus loads the test class a second time in its own class loader and runs its
 * static initialisers again. The details of the database are therefore kept in the JVM's system properties, under
 * the name of the calling test class, and a daemon thread of that name owns the container and stops it when it is
 * interrupted: the second copy of the test class finds the container the first one started instead of starting
 * another.
 */
final class MigratedDatabase implements AutoCloseable {

    static final String LOGGER = "app.rekord.application.security.FirstAdminBootstrap";

    static final String ACTIVE_ADMIN = """
            insert into organizations (id, name, slug)
            values ('00000000-0000-0000-0000-000000000010', 'Owner Org', 'owner-org');
            insert into users (id, email, password_hash, display_name, status)
            values ('00000000-0000-0000-0000-000000000011', 'owner@example.com', 'x', 'Owner', 'ACTIVE');
            insert into memberships (id, org_id, user_id, role, status)
            values ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000010',
                    '00000000-0000-0000-0000-000000000011', 'ADMIN', 'ACTIVE')""";

    private static final String SEPARATOR = "";

    private final String key;
    private final String[] connection;
    private final List<String> rowsBeforeStart;

    MigratedDatabase(String... sqlAfterMigration) {
        key = "migrated-database." + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass().getName();
        if (System.getProperty(key) == null) {
            StartupDatabase database = new StartupDatabase();
            Flyway.configure()
                    .dataSource(database.jdbcUrl(), database.username(), database.password())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            for (String sql : sqlAfterMigration) {
                database.execute(sql);
            }
            String[] details = {database.jdbcUrl(), database.username(), database.password()};
            System.setProperty(key + ".rows", String.join(SEPARATOR, rowsOf(details)));
            System.setProperty(key, String.join(SEPARATOR, details));
            Thread owner = new Thread(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    database.close();
                }
            }, key);
            owner.setDaemon(true);
            owner.start();
        }
        connection = System.getProperty(key).split(SEPARATOR, -1);
        String before = System.getProperty(key + ".rows");
        rowsBeforeStart = before.isEmpty() ? List.of() : List.of(before.split(SEPARATOR, -1));
    }

    String jdbcUrl() {
        return connection[0];
    }

    String username() {
        return connection[1];
    }

    String password() {
        return connection[2];
    }

    List<String> column(String sql) {
        return column(connection, sql);
    }

    long count(String table) {
        return Long.parseLong(column("select count(*) from " + table).get(0));
    }

    /** Every row of every table as text, to look for a value anywhere. */
    List<String> allRowsAsText() {
        return rowsOf(connection);
    }

    /** Every row of every table as it was when the application was about to start. */
    List<String> rowsBeforeStart() {
        return rowsBeforeStart;
    }

    /** The records of the bootstrap logger. */
    static List<LogRecord> bootstrapRecords(List<LogRecord> all) {
        return all.stream().filter(record -> LOGGER.equals(record.getLoggerName())).toList();
    }

    @Override
    public void close() {
        Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> key.equals(thread.getName()))
                .forEach(Thread::interrupt);
    }

    private static List<String> rowsOf(String[] connection) {
        List<String> rows = new ArrayList<>();
        for (String table : column(connection, """
                select table_name from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE'
                  and table_name <> 'flyway_schema_history'""")) {
            rows.addAll(column(connection, "select t::text from " + table + " t"));
        }
        return rows;
    }

    private static List<String> column(String[] connection, String sql) {
        List<String> values = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(connection[0], connection[1], connection[2]);
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
}
