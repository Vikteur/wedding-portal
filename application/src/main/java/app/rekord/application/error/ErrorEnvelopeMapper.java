package app.rekord.application.error;

import app.rekord.api.model.Error;
import app.rekord.api.model.ErrorDetail;
import app.rekord.domain.shared.error.RekordException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.util.stream.Collectors;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Every refusal leaves as {@code {"detail":{"code","message"}}}, byte for byte as rekord-api writes it. */
public class ErrorEnvelopeMapper {

    @ServerExceptionMapper
    public RestResponse<Error> onRekordException(RekordException e) {
        return envelope(ErrorStatusTable.statusOf(e), e.code().name(), e.getMessage());
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

    /** Unknown route, unconvertible path parameter, or someone else's wedding: the same answer, so ids can't be probed. */
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
