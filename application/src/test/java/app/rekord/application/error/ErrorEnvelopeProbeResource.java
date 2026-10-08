package app.rekord.application.error;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Test-only: throws what rekord-api's probe threw, with the oracle's messages. */
@Path("/test-only/error-envelope")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorEnvelopeProbeResource {

    @GET
    @Path("/family/not-found")
    public String notFound() {
        throw new NotFoundException(ErrorCode.NO_WEDDING, "There is no such wedding.");
    }

    @GET
    @Path("/family/rejected")
    public String rejected() {
        throw new RejectedException(
                RejectedException.Kind.CONFLICT, ErrorCode.LIST_FULL, "Their top 20 is full \u2014 all 20 spots are taken.");
    }

    @GET
    @Path("/family/not-permitted")
    public String notPermitted() {
        throw new NotPermittedException(ErrorCode.BAD_CREDENTIALS, "That email and password do not match an account.");
    }

    @GET
    @Path("/family/upstream-unavailable")
    public String upstreamUnavailable() {
        throw new UpstreamUnavailableException(
                ErrorCode.SEARCH_UNAVAILABLE, "Song search is offline \u2014 your text is saved exactly as typed.");
    }
}
