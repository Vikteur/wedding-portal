package app.rekord.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SystemProvidersTest {

    @Test
    void the_clock_is_the_utc_system_clock() {
        Clock clock = new ClockProducer().clock();

        Instant before = Clock.systemUTC().instant();
        Instant read = clock.instant();
        Instant after = Clock.systemUTC().instant();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(read).isBetween(before, after);
    }

    @Test
    void ids_are_random_version_4_uuids() {
        RandomIdGenerator generator = new RandomIdGenerator();
        Set<UUID> seen = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            UUID id = generator.newId();
            assertThat(id.version()).isEqualTo(4);
            assertThat(id.variant()).isEqualTo(2);
            seen.add(id);
        }

        assertThat(seen).hasSize(1000);
    }

    @Test
    void random_bytes_have_the_requested_length_and_differ() {
        SecureRandomTokenGenerator generator = new SecureRandomTokenGenerator();

        byte[] first = generator.randomBytes(16);
        byte[] second = generator.randomBytes(16);

        assertThat(first).hasSize(16);
        assertThat(generator.randomBytes(32)).hasSize(32);
        assertThat(Arrays.equals(first, second)).isFalse();
    }

    @Test
    void random_bytes_refuse_a_negative_count() {
        SecureRandomTokenGenerator generator = new SecureRandomTokenGenerator();

        assertThatThrownBy(() -> generator.randomBytes(-1)).isInstanceOf(RuntimeException.class);
    }
}
