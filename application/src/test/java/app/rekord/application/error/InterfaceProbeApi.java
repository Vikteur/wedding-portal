package app.rekord.application.error;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Test-only (TASK-5.7): the shape of every real resource, where the {@code @Path} sits on an interface and not on the
 * class that implements it.
 */
@Path("/test-only/error-envelope-interface")
public interface InterfaceProbeApi {

    @GET
    @Path("/cyclic-cause")
    String cyclicCause();
}
