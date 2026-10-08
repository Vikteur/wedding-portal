package app.rekord.application.error;

import jakarta.enterprise.context.ApplicationScoped;

/** Test-only (TASK-5.7): like HealthResource, a bean with no {@code @Path} of its own. */
@ApplicationScoped
public class InterfaceProbeResource implements InterfaceProbeApi {

    @Override
    public String cyclicCause() {
        throw ErrorEnvelopeProbeResource.chain(true);
    }
}
