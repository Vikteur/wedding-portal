package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("quarkus-unit-test")
class FlywayRefusesADatabaseWithoutHistoryIT {

    // Given: a database that already holds a table and no flyway_schema_history
    static final StartupDatabase DB = new StartupDatabase("create table stray (id int)");

    @RegisterExtension
    static final QuarkusUnitTest app = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.jdbc.url", DB.jdbcUrl())
            .overrideConfigKey("quarkus.datasource.username", DB.username())
            .overrideConfigKey("quarkus.datasource.password", DB.password())
            .withApplicationRoot(jar -> jar.addAsResource("application.properties")
                    .addAsResource("db/migration/V1__baseline.sql"))
            .assertException(e -> {
                // Then: Flyway refuses the non-empty schema that has no history table
                StringBuilder chain = new StringBuilder();
                for (Throwable t = e; t != null; t = t.getCause()) {
                    chain.append(t.getMessage()).append('\n');
                }
                assertThat(chain.toString().toLowerCase()).contains("no schema history table");
            });

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    @Test
    void start_up_fails_and_the_database_is_left_untouched() {
        // When: the failed start-up is over

        // Then: nothing was adopted or baselined
        List<String> history = DB.column("select to_regclass('public.flyway_schema_history')::text");
        assertThat(history).containsExactly((String) null);
        assertThat(DB.column("select to_regclass('public.stray')::text")).containsExactly("stray");
    }
}
