package app.rekord.application.error;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import app.rekord.api.model.InviteAccept;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

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

    @GET
    @Path("/auth-failed")
    public String authFailed() {
        throw new AuthenticationFailedException();
    }

    @GET
    @Path("/unauthorized")
    public String unauthorized() {
        throw new UnauthorizedException();
    }

    @GET
    @Path("/forbidden")
    public String forbidden() {
        throw new jakarta.ws.rs.ForbiddenException("No organisation on this session.");
    }

    @GET
    @Path("/uuid/{id}")
    public String uuid(@PathParam("id") UUID id) {
        return id.toString();
    }

    /** Takes the generated DTO, whose minLength constraints are what breaks. */
    @POST
    @Path("/invite-accept")
    @Consumes(MediaType.APPLICATION_JSON)
    public void inviteAccept(@Valid @NotNull InviteAccept body) {
        // reaching here means validation did not run
    }

    @GET
    @Path("/web-application-exception")
    public String webApplicationException() {
        throw new jakarta.ws.rs.WebApplicationException(409);
    }
}
