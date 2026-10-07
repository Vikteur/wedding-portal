package app.rekord.application.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Liveness: answers {@code {"ok":true}} to anyone, with no session.
 *
 * <p>It is open on purpose (contract {@code security: []}) and checks no dependency (BR-OPS-21, proven in
 * P0-E04-T05).
 *
 * <p>P0-E02-T03 replaces the {@link Health} record and the JAX-RS annotations with the generated
 * {@code HealthApi} and {@code app.rekord.api.model.Health}.
 */
@Path("/health")
@ApplicationScoped
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Health health() {
        return new Health(true);
    }
}
