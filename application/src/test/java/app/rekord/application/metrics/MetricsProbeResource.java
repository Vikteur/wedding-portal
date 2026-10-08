package app.rekord.application.metrics;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Test-only. A route with a path parameter, to show a metric label holds the template and never the id. */
@Path("/test-only/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsProbeResource {

    @GET
    @Path("/{id}")
    public String byId(@PathParam("id") String id) {
        return "{\"ok\":true}";
    }
}
