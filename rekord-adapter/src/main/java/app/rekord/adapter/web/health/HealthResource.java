package app.rekord.adapter.web.health;

import app.rekord.api.HealthApi;
import app.rekord.api.model.Health;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Liveness: answers {@code {"ok":true}} to anyone, with no session.
 *
 * <p>It is open on purpose (contract {@code security: []}) and checks no dependency (BR-OPS-21, proven by
 * {@code ReadinessIT} and {@code HealthResourceDependenciesTest}). Path, verb and media type
 * come from the generated {@link HealthApi}, so a contract change that touches this operation or the {@link Health} model stops this class from compiling.
 */
@ApplicationScoped
public class HealthResource implements HealthApi {

    @Override
    public Health health() {
        return new Health().ok(true);
    }
}
