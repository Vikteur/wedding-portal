package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("quarkus-unit-test")
class HibernateValidateRefusesAnUnmigratedEntityIT {

    // Given: an empty database and an entity that no migration created a table for
    static final StartupDatabase DB = new StartupDatabase();

    @RegisterExtension
    static final QuarkusUnitTest app = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.jdbc.url", DB.jdbcUrl())
            .overrideConfigKey("quarkus.datasource.username", DB.username())
            .overrideConfigKey("quarkus.datasource.password", DB.password())
            .overrideConfigKey("quarkus.hibernate-orm.mapping-files", "stray-entity-orm.xml")
            .withApplicationRoot(jar -> jar.addPackages(true, "app.rekord.adapter")
                    .addClass(StrayEntity.class)
                    .addAsResource("stray-entity-orm.xml")
                    .addAsResource("application.properties")
                    .addAsResource("db/migration/V1__baseline.sql"))
            .assertException(e -> {
                // Then: Hibernate's schema validation rejects the missing table
                StringBuilder chain = new StringBuilder();
                for (Throwable t = e; t != null; t = t.getCause()) {
                    chain.append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
                }
                assertThat(chain.toString())
                        .contains("SchemaManagementException")
                        .contains("missing table [stray_entity]");
            });

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    @Test
    void start_up_fails_and_hibernate_sent_no_ddl() {
        // When: the failed start-up is over

        // Then: Flyway ran V1 and Hibernate created nothing
        assertThat(DB.column("select table_name from information_schema.tables where table_name = 'stray_entity'"))
                .isEmpty();
        assertThat(DB.column("select version from flyway_schema_history")).containsExactly("1");
    }
}
