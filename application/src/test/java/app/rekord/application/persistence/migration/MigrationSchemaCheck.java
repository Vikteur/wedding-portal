package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

/**
 * The test every versioned migration has: V&lt;version&gt;MigrationIT extends this class, names its version and
 * declares the schema shape {@link SchemaSnapshot} reads (tables, columns, constraints, column defaults and identity
 * settings, the indexes that back no constraint of {@code public}, the enum types with their labels, and the names of
 * other schemas) that the history up to and including that version leaves on an empty container. What
 * {@code SchemaSnapshot} cannot see, such as sequences, views, functions and extensions, is not checked here: see its
 * Javadoc, and test those separately ({@code SchemaMigrationIT} does views, sequences and extensions).
 * Not discovered on its own (the name does not end in IT); {@code MigrationCoverageTest} fails the build for a V file
 * without such a subclass.
 */
abstract class MigrationSchemaCheck {

    private static final String PRODUCTION_LOCATION = "classpath:db/migration";

    /**
     * The version of the migration this class tests, written with dots: {@code "1.1"} for {@code V1_1__x.sql} or
     * {@code V1.1__x.sql} (Flyway treats underscores and dots in a version alike).
     */
    protected abstract String version();

    /** The schema shape {@link SchemaSnapshot} reads, after the migration. */
    protected abstract SchemaSnapshot expected();

    @Test
    void applies_the_history_up_to_and_including_its_migration_on_an_empty_container_and_finds_the_declared_schema()
            throws Exception {
        // Given an empty container of its own
        try (MigrationDatabase database = new MigrationDatabase()) {
            check(database, PRODUCTION_LOCATION, version(), expected());
        }
    }

    /** The check itself, on any location, so {@code MigrationSchemaCheckIT} can show it fail on the fixtures. */
    static void check(MigrationDatabase database, String location, String version, SchemaSnapshot expected)
            throws Exception {
        assertThat(database.snapshot()).as("empty container").isEqualTo(SchemaSnapshot.empty());

        // When the history is migrated up to and including this version
        database.migrateTo(version, location);

        // Then every resolved version up to it was applied in order, none skipped
        MigrationVersion target = MigrationVersion.fromVersion(version);
        var resolved = Arrays.stream(Flyway.configure()
                        .dataSource(database.container().getJdbcUrl(), database.container().getUsername(),
                                database.container().getPassword())
                        .locations(location)
                        .load()
                        .info()
                        .all())
                .map(info -> info.getVersion())
                .filter(v -> v != null && v.compareTo(target) <= 0)
                .map(MigrationVersion::getVersion)
                .toList();
        assertThat(resolved).as("resolved versions include " + version).contains(target.getVersion());
        assertThat(database.historyVersions()).as("applied history").isEqualTo(resolved);

        // And the schema is the declared one
        assertThat(database.snapshot()).isEqualTo(expected);
    }
}
