package app.rekord.application.health;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/** A real datasource on a PostgreSQL that the test owns, with Dev Services off. */
public class ReadinessTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.devservices.enabled", "false");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(StoppablePostgres.class));
    }
}
