package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.api.model.Error;
import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.RekordException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
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

    @Test
    void both_framework_401s_answer_not_signed_in() {
        for (RestResponse<Error> response : java.util.List.of(
                mapper.onAuthenticationFailed(new AuthenticationFailedException()),
                mapper.onUnauthorized(new UnauthorizedException()))) {
            assertEnvelope(response, 401, "NOT_SIGNED_IN", "Sign in to continue.");
        }
    }

    @Test
    void a_forbidden_answers_403_forbidden() {
        assertEnvelope(
                mapper.onForbidden(new jakarta.ws.rs.ForbiddenException("No organisation on this session.")),
                403,
                "FORBIDDEN",
                "This is not yours to open.");
    }

    @Test
    void a_not_found_answers_404_no_wedding_with_nothing_here() {
        assertEnvelope(
                mapper.onNotFound(new jakarta.ws.rs.NotFoundException()), 404, "NO_WEDDING", "There is nothing here.");
    }

    @Test
    void the_role_denied_forbidden_of_quarkus_security_has_no_mapper_so_its_framework_body_stays() {
        boolean mapped = Arrays.stream(ErrorEnvelopeMapper.class.getDeclaredMethods())
                .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                .anyMatch(t -> t == io.quarkus.security.ForbiddenException.class);
        assertThat(mapped).isFalse();
    }

    private static void assertEnvelope(RestResponse<Error> response, int status, String code, String message) {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity().getDetail().getCode().name()).isEqualTo(code);
        assertThat(response.getEntity().getDetail().getMessage()).isEqualTo(message);
    }
}
