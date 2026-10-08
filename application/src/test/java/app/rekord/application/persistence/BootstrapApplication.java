package app.rekord.application.persistence;

import io.quarkus.test.QuarkusUnitTest;

/** The Quarkus unit test of the first-admin bootstrap: the packages it needs and the shipped settings. */
final class BootstrapApplication {

    private BootstrapApplication() {}

    static QuarkusUnitTest on(QuarkusUnitTest test, Class<?> testClass, MigratedDatabase database) {
        return test
                .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
                .overrideConfigKey("quarkus.datasource.jdbc.url", database.jdbcUrl())
                .overrideConfigKey("quarkus.datasource.username", database.username())
                .overrideConfigKey("quarkus.datasource.password", database.password())
                .withApplicationRoot(jar -> jar.addPackages(true, "app.rekord.adapter", "app.rekord.usecase")
                        .addPackages(true, "app.rekord.application.config", "app.rekord.application.security")
                        .addAsResource("application.properties")
                        .addAsResource("db/migration/V1__baseline.sql")
                        .addAsResource("db/migration/V2__identity.sql")
                        .addClasses(testClass, BootstrapApplication.class, MigratedDatabase.class, StartupDatabase.class))
                .setLogRecordPredicate(record -> true);
    }
}
