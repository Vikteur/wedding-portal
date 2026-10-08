package app.rekord.application.error;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Test-only (TASK-5.7): the {@code @Path} sits on an abstract superclass, so the class that serves it has none of its
 * own and implements no interface that has one. This pins the superclass step of the extension's search for a resource.
 */
@Path("/test-only/error-envelope-superclass")
public abstract class SuperclassProbeBase {

    @GET
    @Path("/cyclic-cause")
    public abstract String cyclicCause();
}
