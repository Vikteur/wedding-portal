package app.rekord.application.error;

import app.rekord.api.model.Error;
import app.rekord.api.model.ErrorDetail;
import app.rekord.domain.shared.error.RekordException;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Every refusal leaves as {@code {"detail":{"code","message"}}}, byte for byte as rekord-api writes it. */
public class ErrorEnvelopeMapper {

    @ServerExceptionMapper
    public RestResponse<Error> onRekordException(RekordException e) {
        return envelope(ErrorStatusTable.statusOf(e), e.code().name(), e.getMessage());
    }

    private static RestResponse<Error> envelope(int status, String code, String message) {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode(app.rekord.api.model.ErrorCode.fromString(code));
        detail.setMessage(message);
        Error error = new Error();
        error.setDetail(detail);
        return RestResponse.ResponseBuilder.create(RestResponse.Status.fromStatusCode(status), error)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
