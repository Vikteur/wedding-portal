package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.api.model.Error;
import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.RekordException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import jakarta.ws.rs.core.MediaType;
import java.util.stream.Stream;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ErrorEnvelopeMapperTest {

    private final ErrorEnvelopeMapper mapper = new ErrorEnvelopeMapper();

    static Stream<Arguments> families() {
        return Stream.of(
                Arguments.of(new NotFoundException(ErrorCode.NO_WEDDING, "There is no such wedding."), 404),
                Arguments.of(
                        new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.LIST_FULL, "Their top 20 is full."),
                        409),
                Arguments.of(new NotPermittedException(ErrorCode.BAD_CREDENTIALS, "Wrong email or password."), 401),
                Arguments.of(
                        new UpstreamUnavailableException(ErrorCode.SEARCH_UNAVAILABLE, "Song search is offline."), 503));
    }

    @ParameterizedTest
    @MethodSource("families")
    void a_family_error_answers_its_table_status_with_its_code_and_message(RekordException e, int status) {
        // When
        RestResponse<Error> response = mapper.onRekordException(e);

        // Then
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        Error entity = response.getEntity();
        assertThat(entity.getDetail().getCode().name()).isEqualTo(e.code().name());
        assertThat(entity.getDetail().getMessage()).isEqualTo(e.getMessage());
    }

    @Test
    void a_pair_without_a_table_row_does_not_answer_404() {
        // Given: NO_WEDDING is a NotFound code; as a Rejected it has no row
        var e = new RejectedException(RejectedException.Kind.VALIDATION, ErrorCode.NO_WEDDING, "No such wedding.");

        // When
        int status;
        try {
            status = mapper.onRekordException(e).getStatus();
        } catch (IllegalArgumentException noRow) {
            return;
        }

        // Then
        assertThat(status).isNotEqualTo(404);
    }
}
