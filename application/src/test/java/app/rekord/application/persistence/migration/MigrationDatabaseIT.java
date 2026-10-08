package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.application.persistence.FreshDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationDatabaseIT {

    private static final String FIXTURES = "classpath:migration-fixtures";

    @Test
    void starts_an_empty_postgres_17_alpine_container_that_is_not_reused() throws Exception {
        // Given a migration database
        try (MigrationDatabase database = new MigrationDatabase();
                Connection c = database.connection()) {

            // When its server is asked
            String versionNum = scalar(c, "show server_version_num");
            String version = scalar(c, "select version()");
            String history = scalar(c, "select count(*) from information_schema.tables where table_name = 'flyway_schema_history'");

            // Then it is a container of this run, PostgreSQL 17 on alpine, and nothing is in it
            assertThat(database.container().isShouldBeReused()).isFalse();
            assertThat(versionNum).startsWith("17");
            assertThat(version).contains("musl");
            assertThat(database.snapshot()).isEqualTo(SchemaSnapshot.empty());
            assertThat(history).isEqualTo("0");
        }
    }

    @Test
    void the_container_was_started_by_this_run() throws Exception {
        // Given a migration database
        try (MigrationDatabase database = new MigrationDatabase();
                Connection c = database.connection()) {

            // When / Then the server started after this JVM did
            FreshDatabase.assertStartedAfterThisJvm(c);
        }
    }

    @Test
    void migrate_to_applies_the_history_up_to_and_including_the_target_and_no_further() throws Exception {
        // Given the fixture migrations V1 (fixture_first) and V2 (fixture_second)
        try (MigrationDatabase first = new MigrationDatabase()) {
            // When migrating to 1
            first.migrateTo("1", FIXTURES);

            // Then only fixture_first exists, with one history row
            assertThat(first.snapshot().tables()).containsExactly("fixture_first");
            assertThat(first.historyVersions()).containsExactly("1");
        }
        try (MigrationDatabase second = new MigrationDatabase()) {
            // When migrating a second fresh database to 2
            second.migrateTo("2", FIXTURES);

            // Then both tables exist, with two history rows
            assertThat(second.snapshot().tables()).containsExactly("fixture_first", "fixture_second");
            assertThat(second.historyVersions()).isEqualTo(List.of("1", "2"));
        }
    }

    @Test
    void history_versions_refuses_a_history_with_a_failed_row() throws Exception {
        // Given a migrated fixture history with a failed row added after it
        try (MigrationDatabase database = new MigrationDatabase()) {
            database.migrateTo("1", FIXTURES);
            try (Connection c = database.connection(); Statement s = c.createStatement()) {
                s.execute("""
                        insert into flyway_schema_history
                          (installed_rank, version, description, type, script, installed_by, execution_time, success)
                        values (2, '2', 'fixture second', 'SQL', 'V2__fixture_second.sql', 'test', 0, false)""");
            }

            // When / Then
            assertThatThrownBy(database::historyVersions)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("migration 2 failed");
        }
    }

    private static String scalar(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getString(1);
        }
    }
}
