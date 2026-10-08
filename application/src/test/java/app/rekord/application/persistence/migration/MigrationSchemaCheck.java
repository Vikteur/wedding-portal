package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

/**
 * The test every versioned migration has: V&lt;version&gt;MigrationIT extends this class, names its version and
 * declares the full schema that the history up to and including that version leaves on an empty container.
 * Not discovered on its own (the name does not end in IT); {@code MigrationCoverageTest} fails the build for a V file
 * without such a subclass.
 */
abstract class MigrationSchemaCheck {

    /** The version of the migration this class tests, as in the V file name (dots for underscores). */
    protected abstract String version();

    /** The whole schema after the migration. */
    protected abstract SchemaSnapshot expected();

    @Test
    void applies_the_history_up_to_and_including_its_migration_on_an_empty_container_and_finds_the_declared_schema()
            throws Exception {
        // Given an empty container of its own
        try (MigrationDatabase database = new MigrationDatabase()) {
            assertThat(database.snapshot()).as("empty container").isEqualTo(SchemaSnapshot.empty());

            // When the history is migrated up to and including this version
            database.migrateTo(version());

            // Then every resolved version up to it was applied in order, none skipped
            MigrationVersion target = MigrationVersion.fromVersion(version());
            var resolved = Arrays.stream(Flyway.configure()
                            .dataSource(database.container().getJdbcUrl(), database.container().getUsername(),
                                    database.container().getPassword())
                            .locations("classpath:db/migration")
                            .load()
                            .info()
                            .all())
                    .map(info -> info.getVersion())
                    .filter(v -> v != null && v.compareTo(target) <= 0)
                    .map(MigrationVersion::getVersion)
                    .toList();
            assertThat(resolved).as("resolved versions include " + version()).contains(target.getVersion());
            assertThat(database.historyVersions()).as("applied history").isEqualTo(resolved);

            // And the schema is the declared one
            assertThat(database.snapshot()).isEqualTo(expected());
        }
    }
}
