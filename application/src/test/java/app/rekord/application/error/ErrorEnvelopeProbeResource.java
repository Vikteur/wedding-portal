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

/**
 * Test-only. The family, auth-failed, forbidden, web-application-exception and uuid probes replay what rekord-api's
 * probe threw, with the oracle's messages; invite-accept replays the oracle's invalid request body, and unauthorized
 * throws the exception behind the oracle's anonymous 401. The role-denied probe has no oracle: the framework answers
 * it. The unhandled, service-unavailable, internal-server-error, pair-without-row and cyclic-cause probes throw
 * sentinel chains (or sentinel messages), and cyclic-from-interceptor has an interceptor throw one, to prove none of
 * them reaches a log record.
 */
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

    /** The role-denied refusal: no mapper of ours names it, so the framework answers it (TASK-6.2 records that body). */
    @GET
    @Path("/role-denied")
    public String roleDenied() {
        throw new io.quarkus.security.ForbiddenException();
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
        return chain(true);
    }

    /**
     * With {@code withCycle}, the SQLException's cause is the top exception again. Proven in TASK-5.7: Quarkus REST
     * 3.39.1 does not survive that. After the mapper has answered, {@code RuntimeExceptionMapper.mapException} calls
     * {@code logBlockingErrorIfRequired}, then {@code isBlockingProblem}, then {@code isKnownProblem}
     * ({@code RuntimeExceptionMapper.java:187}), which follows {@code getCause()} in {@code while (e != null)} with no
     * visited set: the worker thread spins at full CPU and no response is written. Only a chain thrown from a resource
     * method is now safe, because {@code AcyclicCauseInterceptor} rethrows a redacted, acyclic copy of it; the
     * {@code /cyclic-cause} probe and {@code ErrorEnvelopeMapperIT} hold that. A cycle raised anywhere else (a filter,
     * a body reader, a parameter conversion) would still hang the thread, and {@code chain(false)} stays the safe
     * input for a probe that is not about the guard. See docs/memory.md, section TASK-5.7.
     */
    static IllegalStateException chain(boolean withCycle) {
        SQLException sql = new SQLException(
                "ERROR: duplicate key Key (email)=(member@example.com) +12025550100 Testa Persona 4821-7735 pw-test-0001");
        IllegalStateException top = new IllegalStateException(
                "insert into wedding_member (email, token) values ('member@example.com', 'tok-example-123')", sql);
        if (withCycle) {
            sql.initCause(top);
        }
        top.addSuppressed(new IllegalArgumentException("suppressed member@example.com tok-example-123 pw-test-0001"));
        return top;
    }

    @GET
    @Path("/unhandled")
    public String unhandled() {
        throw chain(false);
    }

    @GET
    @Path("/service-unavailable")
    public String serviceUnavailable() {
        throw new jakarta.ws.rs.ServiceUnavailableException("upstream said member@example.com tok-example-123");
    }

    /** Exactly 500: the first status the catch-all treats as a server fault rather than a client refusal. */
    @GET
    @Path("/internal-server-error")
    public String internalServerError() {
        throw new jakarta.ws.rs.InternalServerErrorException("upstream said member@example.com tok-example-123");
    }

    /** NO_WEDDING is a NotFound code, so as a Rejected it has no ErrorStatusTable row. */
    @GET
    @Path("/pair-without-row")
    public String pairWithoutRow() {
        throw new RejectedException(
                RejectedException.Kind.VALIDATION, ErrorCode.NO_WEDDING, "no row for member@example.com tok-example-123");
    }

    /** The sentinel chain with its cause cycle (TASK-5.7): only the interceptor keeps Quarkus REST from looping on it. */
    @GET
    @Path("/cyclic-cause")
    public String cyclicCause() {
        throw chain(true);
    }

    /**
     * The same chain, thrown by another interceptor before this body runs (TASK-5.7): the guard keeps Quarkus REST from
     * looping on it only while it is the outermost interceptor of the method.
     */
    @ThrowsCyclicChain
    @GET
    @Path("/cyclic-from-interceptor")
    public String cyclicFromInterceptor() {
        return "not reached: CyclicChainInterceptor throws first";
    }
}
