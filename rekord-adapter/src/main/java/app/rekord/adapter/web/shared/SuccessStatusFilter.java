package app.rekord.adapter.web.shared;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.OptionalInt;

/**
 * Applies the code recorded in {@link SuccessStatus}; a 204 also loses its entity, so no body is written.
 *
 * <p>Only a successful answer is changed: when the operation was refused after it named its code, the refusal's
 * status stands.
 */
@Provider
public class SuccessStatusFilter implements ContainerResponseFilter {

    private final SuccessStatus successStatus;

    // Public constructor injection: the bean class may live in another class loader (QuarkusUnitTest), where a
    // package-private field is out of reach.
    @Inject
    public SuccessStatusFilter(SuccessStatus successStatus) {
        this.successStatus = successStatus;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        OptionalInt code = successStatus.status();
        if (code.isEmpty() || response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            return;
        }
        response.setStatus(code.getAsInt());
        if (code.getAsInt() == 204) {
            response.setEntity(null);
        }
    }
}
