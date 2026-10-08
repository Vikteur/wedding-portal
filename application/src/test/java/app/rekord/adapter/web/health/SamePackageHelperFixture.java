package app.rekord.adapter.web.health;

import jakarta.inject.Inject;
import javax.sql.DataSource;

/**
 * Test-only, for {@code HealthResourceDependenciesTest}: a resource next to {@link HealthResource} that reaches a
 * datasource through a helper of its own package. Never a bean (no scope annotation).
 */
public final class SamePackageHelperFixture {

    private SamePackageHelperFixture() {}

    public static class ResourceWithAHelper {
        @Inject
        DatabaseHelper helper;
    }

    public static class DatabaseHelper {
        @Inject
        DataSource dataSource;
    }
}
