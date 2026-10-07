package app.rekord.application;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/** Resource-test profile without datasource, Flyway, Hibernate or Dev Services (PIN-AC-0535). */
public class ResourceTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.devservices.enabled", "false",
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.datasource.active", "false",
                "quarkus.flyway.active", "false",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.hibernate-orm.active", "false");
    }
}
