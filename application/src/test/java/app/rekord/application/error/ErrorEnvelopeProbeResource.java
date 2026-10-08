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
import java.sql.SQLException;
import java.util.List;
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

    /** Throwaway values only, in the shapes UD-19.f keeps out of logs. */
    static final List<String> SENTINELS = List.of(
            "member@example.com",
            "tok-example-123",
            "+12025550100",
            "Testa Persona",
            "4821-7735",
            "pw-test-0001",
            "insert into wedding_member");

    /** An exception whose messages, cause, suppressed exception and cause cycle all carry the sentinels. */
    static IllegalStateException chain() {
        SQLException sql = new SQLException(
                "ERROR: duplicate key Key (email)=(member@example.com) +12025550100 Testa Persona 4821-7735 pw-test-0001");
        IllegalStateException top = new IllegalStateException(
                "insert into wedding_member (email, token) values ('member@example.com', 'tok-example-123')", sql);
        sql.initCause(top);
        top.addSuppressed(new IllegalArgumentException("suppressed member@example.com tok-example-123 pw-test-0001"));
        return top;
    }

    @GET
    @Path("/unhandled")
    public String unhandled() {
        throw chain();
    }

    @GET
    @Path("/service-unavailable")
    public String serviceUnavailable() {
        throw new jakarta.ws.rs.ServiceUnavailableException("upstream said member@example.com tok-example-123");
    }

    /** NO_WEDDING is a NotFound code, so as a Rejected it has no ErrorStatusTable row. */
    @GET
    @Path("/pair-without-row")
    public String pairWithoutRow() {
        throw new RejectedException(
                RejectedException.Kind.VALIDATION, ErrorCode.NO_WEDDING, "no row for member@example.com tok-example-123");
    }
}
