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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
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

    // --- bean validation: rekord-api's rendering, no sorting (sorting is TASK-5.6's deviation) ---

    static class Account {
        @NotBlank String name;
        @Size(min = 8) String password;

        Account(String name, String password) {
            this.name = name;
            this.password = password;
        }
    }

    static class Line {
        @NotBlank String name;

        Line(String name) {
            this.name = name;
        }
    }

    static class Order {
        @Valid List<Line> lines;

        Order(List<Line> lines) {
            this.lines = lines;
        }
    }

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private <T> RestResponse<Error> validationAnswer(T bean) {
        return mapper.onConstraintViolation(new ConstraintViolationException(VALIDATOR.validate(bean)));
    }

    @Test
    void two_violations_render_as_last_segment_and_message_joined_by_semicolon() {
        RestResponse<Error> response = validationAnswer(new Account("", "short"));

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity().getDetail().getCode().name()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getEntity().getDetail().getMessage())
                .isIn(
                        "name must not be blank; password size must be between 8 and 2147483647",
                        "password size must be between 8 and 2147483647; name must not be blank");
    }

    @Test
    void a_nested_path_renders_its_last_segment_only() {
        RestResponse<Error> response = validationAnswer(new Order(List.of(new Line("ok"), new Line(""))));

        assertThat(response.getEntity().getDetail().getMessage()).isEqualTo("name must not be blank");
    }

    @Test
    void identical_renderings_from_different_paths_appear_once() {
        RestResponse<Error> response = validationAnswer(new Order(List.of(new Line(""), new Line(""))));

        assertThat(response.getEntity().getDetail().getMessage()).isEqualTo("name must not be blank");
    }

    @Test
    void a_single_violation_has_no_separator() {
        RestResponse<Error> response = validationAnswer(new Account("Test Persona", "short"));

        assertThat(response.getEntity().getDetail().getMessage())
                .isEqualTo("password size must be between 8 and 2147483647");
    }

    @Test
    void a_web_application_exception_below_500_keeps_its_status_with_code_unknown() {
        assertEnvelope(
                mapper.onThrowable(new jakarta.ws.rs.WebApplicationException(409)),
                409,
                "UNKNOWN",
                "That request could not be handled.");
    }

    @Test
    void the_frameworks_405_keeps_its_status_with_code_unknown() {
        assertEnvelope(
                mapper.onThrowable(new jakarta.ws.rs.NotAllowedException("GET")),
                405,
                "UNKNOWN",
                "That request could not be handled.");
    }
}
