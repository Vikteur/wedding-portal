package app.rekord.application.error;

import jakarta.enterprise.context.ApplicationScoped;

/** Test-only (TASK-5.7): a bean with no {@code @Path} of its own, whose abstract superclass carries it. */
@ApplicationScoped
public class SuperclassProbeResource extends SuperclassProbeBase {

    @Override
    public String cyclicCause() {
        throw ErrorEnvelopeProbeResource.chain(true);
    }
}
