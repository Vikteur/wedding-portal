package app.rekord.adapter.web.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SuccessStatusTest {

    @Test
    void before_any_call_no_status_is_set() {
        assertThat(new SuccessStatus().status()).isEqualTo(OptionalInt.empty());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 202, 204})
    void accepts_the_success_codes_and_records_the_last_one(int code) {
        SuccessStatus status = new SuccessStatus();

        status.answer(code);

        assertThat(status.status()).isEqualTo(OptionalInt.of(code));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 199, 205, 300, 400, 404, 500})
    void refuses_any_other_code_and_records_nothing(int code) {
        SuccessStatus status = new SuccessStatus();

        assertThatThrownBy(() -> status.answer(code)).isInstanceOf(IllegalArgumentException.class);
        assertThat(status.status()).isEqualTo(OptionalInt.empty());
    }
}
