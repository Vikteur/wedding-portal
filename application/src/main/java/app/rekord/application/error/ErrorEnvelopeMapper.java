package app.rekord.application.error;

import app.rekord.api.model.Error;
import app.rekord.api.model.ErrorDetail;
import app.rekord.domain.shared.error.RekordException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Every refusal raised inside JAX-RS leaves as {@code {"detail":{"code","message"}}}, byte for byte as rekord-api
 * writes it. Three answers are not ours: the 413 for an oversized body (Vert.x answers it, with no body), the
 * role-denied {@code io.quarkus.security.ForbiddenException} (no mapper names it, so the framework's body stays;
 * TASK-6.2), and a {@code WebApplicationException} that carries its own entity (the framework writes that response as
 * it is).
 */
public class ErrorEnvelopeMapper {

    private static final Logger LOG = Logger.getLogger(ErrorEnvelopeMapper.class);

    @ServerExceptionMapper
    public RestResponse<Error> onRekordException(RekordException e) {
        int status;
        try {
            status = ErrorStatusTable.statusOf(e);
        } catch (IllegalArgumentException noRow) {
            // A (code, family) pair without a row is a programming error: answered as any other unhandled failure.
            return unhandled(e);
        }
        return envelope(status, e.code().name(), e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<Error> onAuthenticationFailed(AuthenticationFailedException e) {
        return envelope(401, "NOT_SIGNED_IN", "Sign in to continue.");
    }

    @ServerExceptionMapper
    public RestResponse<Error> onUnauthorized(UnauthorizedException e) {
        return envelope(401, "NOT_SIGNED_IN", "Sign in to continue.");
    }

    @ServerExceptionMapper
    public RestResponse<Error> onForbidden(ForbiddenException e) {
        return envelope(403, "FORBIDDEN", "This is not yours to open.");
    }

    /**
     * The framework's 404 ({@code jakarta.ws.rs.NotFoundException}): an unknown route or an unconvertible path
     * parameter. A domain "not found" is the {@code RekordException} family and answers through
     * {@link #onRekordException}; a wedding that exists but is someone else's must be refused there with the same code
     * and message as a missing one, so ids can't be probed.
     */
    @ServerExceptionMapper
    public RestResponse<Error> onNotFound(NotFoundException e) {
        return envelope(404, "NO_WEDDING", "There is nothing here.");
    }

    /** One sentence rather than a list of paths, in the violation set's own order: sorting is TASK-5.6's deviation. */
    @ServerExceptionMapper
    public RestResponse<Error> onConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    int dot = path.lastIndexOf('.');
                    return (dot < 0 ? path : path.substring(dot + 1)) + " " + v.getMessage();
                })
                .distinct()
                .collect(Collectors.joining("; "));
        return envelope(422, "VALIDATION_FAILED", message);
    }

    /** The catch-all: a refusal the framework already put a status below 500 on keeps it, anything else is a 500. */
    @ServerExceptionMapper
    public RestResponse<Error> onThrowable(Throwable e) {
        if (e instanceof WebApplicationException w && w.getResponse() != null && w.getResponse().getStatus() < 500) {
            return envelope(w.getResponse().getStatus(), "UNKNOWN", "That request could not be handled.");
        }
        return unhandled(e);
    }

    /** Logs a redacted copy of the cause (class names and frames, no messages), once, and answers 500. */
    private static RestResponse<Error> unhandled(Throwable e) {
        LOG.error("Unhandled exception", RedactedCause.of(e));
        return envelope(500, "UNKNOWN", "Something went wrong at our end.");
    }

    private static RestResponse<Error> envelope(int status, String code, String message) {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode(app.rekord.api.model.ErrorCode.fromString(code));
        detail.setMessage(message);
        Error error = new Error();
        error.setDetail(detail);
        return RestResponse.ResponseBuilder.create(RestResponse.Status.OK, error)
                .status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
