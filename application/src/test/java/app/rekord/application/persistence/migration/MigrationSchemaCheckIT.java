package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The check every {@code V<version>MigrationIT} inherits, run on the fixture migrations: it passes and it fails. */
class MigrationSchemaCheckIT {

    private static final String FIXTURES = "classpath:migration-fixtures";

    @Test
    void the_check_passes_when_the_declared_schema_is_the_one_the_history_leaves() {
        // Given an empty container and the schema fixture V1 leaves
        try (MigrationDatabase database = new MigrationDatabase()) {

            // When / Then
            assertThatCode(() -> MigrationSchemaCheck.check(database, FIXTURES, "1", fixtureFirstOnly()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void the_check_fails_when_the_declared_schema_misses_a_table_of_the_history() {
        // Given an empty container and a declaration that leaves out the table fixture V2 creates
        try (MigrationDatabase database = new MigrationDatabase()) {

            // When / Then
            assertThatThrownBy(() -> MigrationSchemaCheck.check(database, FIXTURES, "2", fixtureFirstOnly()))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("fixture_second");
        }
    }

    @Test
    void the_check_fails_on_a_container_that_is_not_empty() throws Exception {
        // Given a container that already holds a table
        try (MigrationDatabase database = new MigrationDatabase()) {
            try (Connection c = database.connection(); Statement s = c.createStatement()) {
                s.execute("create table left_over (id bigint)");
            }

            // When / Then
            assertThatThrownBy(() -> MigrationSchemaCheck.check(database, FIXTURES, "1", fixtureFirstOnly()))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("empty container");
        }
    }

    private static SchemaSnapshot fixtureFirstOnly() {
        return new SchemaSnapshot(
                List.of(),
                List.of("fixture_first"),
                List.of(new SchemaSnapshot.Column("fixture_first", "id", "bigint", false)),
                List.of(new SchemaSnapshot.Constraint(
                        "fixture_first", "fixture_first_pkey", "PRIMARY KEY", List.of("id"), "")));
    }
}
