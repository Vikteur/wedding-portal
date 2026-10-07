package app.rekord.domain.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RekordExceptionTest {

    private static final List<Class<? extends RekordException>> FAMILIES = List.of(
            NotFoundException.class,
            RejectedException.class,
            NotPermittedException.class,
            UpstreamUnavailableException.class);

    record Factory(String name, BiFunction<ErrorCode, String, RekordException> make) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Factory> factories() {
        return Stream.of(
                new Factory("NotFound", NotFoundException::new),
                new Factory("Rejected", (c, m) -> new RejectedException(RejectedException.Kind.VALIDATION, c, m)),
                new Factory("NotPermitted", NotPermittedException::new),
                new Factory("UpstreamUnavailable", UpstreamUnavailableException::new));
    }

    @Test
    void rekord_exception_is_sealed_and_permits_exactly_the_four_families() {
        assertThat(RekordException.class.isSealed()).isTrue();
        Set<Class<?>> permitted = Arrays.stream(RekordException.class.getPermittedSubclasses())
                .collect(Collectors.toSet());
        assertThat(permitted).containsExactlyInAnyOrderElementsOf(FAMILIES);
    }

    @Test
    void every_family_is_final_and_a_runtime_exception() {
        for (Class<? extends RekordException> family : FAMILIES) {
            assertThat(Modifier.isFinal(family.getModifiers())).as(family.getSimpleName()).isTrue();
            assertThat(RuntimeException.class).isAssignableFrom(family);
        }
        assertThat(RuntimeException.class).isAssignableFrom(RekordException.class);
    }

    @ParameterizedTest
    @MethodSource("factories")
    void each_family_carries_its_code_and_message(Factory factory) {
        RekordException e = factory.make().apply(ErrorCode.NO_LIBRARY, "no library");
        assertThat(e.code()).isEqualTo(ErrorCode.NO_LIBRARY);
        assertThat(e.getMessage()).isEqualTo("no library");
    }

    @Test
    void rejected_carries_its_kind() {
        assertThat(new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.NO_LIBRARY, "x").kind())
                .isEqualTo(RejectedException.Kind.CONFLICT);
        assertThat(new RejectedException(RejectedException.Kind.VALIDATION, ErrorCode.EMPTY_NAME, "x").kind())
                .isEqualTo(RejectedException.Kind.VALIDATION);
    }

    @ParameterizedTest
    @MethodSource("factories")
    void code_and_message_are_required(Factory factory) {
        assertThatThrownBy(() -> factory.make().apply(null, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> factory.make().apply(ErrorCode.NO_LIBRARY, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void no_family_carries_a_status() {
        Stream<Class<?>> classes = Stream.concat(Stream.of(RekordException.class), FAMILIES.stream());
        classes.forEach(c -> {
            Arrays.stream(c.getDeclaredFields()).forEach(f -> assertThat(f.getType())
                    .as(c.getSimpleName() + "." + f.getName())
                    .isNotIn(int.class, Integer.class));
            for (Method m : c.getDeclaredMethods()) {
                assertThat(m.getReturnType()).as(c.getSimpleName() + "." + m.getName())
                        .isNotIn(int.class, Integer.class);
            }
        });
    }
}
