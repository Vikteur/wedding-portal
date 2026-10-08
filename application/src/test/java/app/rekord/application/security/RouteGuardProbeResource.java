package app.rekord.application.security;

import app.rekord.api.model.Health;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only routes under {@code /api/test-only/route-guard} that carry an access annotation. Their bodies count their
 * calls, so a test sees whether the framework refused the visitor before the body ran. Implements no generated
 * interface, so the route guard does not scan it.
 */
@Path("/test-only/route-guard")
@Produces(MediaType.APPLICATION_JSON)
public class RouteGuardProbeResource {

    static final AtomicInteger CALLS = new AtomicInteger();

    @GET
    @Path("/authenticated")
    @Authenticated
    public Health authenticated() {
        CALLS.incrementAndGet();
        return new Health().ok(true);
    }

    @GET
    @Path("/roles-allowed")
    @RolesAllowed("ADMIN")
    public Health rolesAllowed() {
        CALLS.incrementAndGet();
        return new Health().ok(true);
    }
}
