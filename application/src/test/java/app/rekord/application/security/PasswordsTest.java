package app.rekord.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import app.rekord.usecase.shared.port.TokenGenerator;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.bouncycastle.crypto.generators.SCrypt;
import org.junit.jupiter.api.Test;

class PasswordsTest {

    private static final String PASSWORD = "made-up-pass-1234";
    private static final byte[] SALT = HexFormat.of().parseHex("000102030405060708090a0b0c0d0e0f");

    private static final class RecordingTokenGenerator implements TokenGenerator {
        final List<Integer> asked = new ArrayList<>();
        private final byte[] salt;

        RecordingTokenGenerator(byte[] salt) {
            this.salt = salt;
        }

        @Override
        public byte[] randomBytes(int count) {
            asked.add(count);
            return salt.clone();
        }
    }

    private final RecordingTokenGenerator tokens = new RecordingTokenGenerator(SALT);
    private final Passwords passwords = new Passwords(tokens);

    @Test
    void a_hash_has_the_scrypt_format_of_rekord_api() {
        assertThat(passwords.hash(PASSWORD)).matches("^scrypt[$]16384[$]8[$]1[$][0-9a-f]{32}[$][0-9a-f]{64}$");
    }

    @Test
    void the_salt_is_sixteen_bytes_from_the_token_generator() {
        String hash = passwords.hash(PASSWORD);

        assertThat(tokens.asked).containsExactly(16);
        assertThat(hash.split("[$]")[4]).isEqualTo(HexFormat.of().formatHex(SALT));
    }

    @Test
    void a_hash_verifies_its_own_password_and_refuses_another() {
        String hash = passwords.hash(PASSWORD);

        assertThat(passwords.verify(PASSWORD, hash)).isTrue();
        assertThat(passwords.verify("made-up-pass-1235", hash)).isFalse();
        assertThat(passwords.verify("", hash)).isFalse();
    }

    @Test
    void the_rfc_7914_vector_verifies() {
        String saltHex = HexFormat.of().formatHex("SodiumChloride".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        String key = "7023bdcb3afd7348461c06cd81fd38eb" + "fda8fbba904f8e3ea9b543f6545da1f2"
                + "d5432955613f0fcf62d49705242a9af9" + "e61e85dc0d651e40dfcf017b45575887";
        String stored = "scrypt$16384$8$1$" + saltHex + "$" + key;

        assertThat(passwords.verify("pleaseletmein", stored)).isTrue();
        assertThat(passwords.verify("pleaseletmeim", stored)).isFalse();
    }

    @Test
    void verify_answers_false_on_null_or_malformed_values() {
        String good = passwords.hash(PASSWORD);
        String saltHex = HexFormat.of().formatHex(SALT);
        String keyHex = good.split("[$]")[5];

        assertThat(passwords.verify(null, good)).isFalse();
        assertThat(passwords.verify(PASSWORD, null)).isFalse();
        assertThat(passwords.verify(PASSWORD, "")).isFalse();
        assertThat(passwords.verify(PASSWORD, "scrypt$16384$8$1$" + saltHex)).isFalse();
        assertThat(passwords.verify(PASSWORD, good + "$extra")).isFalse();
        assertThat(passwords.verify(PASSWORD, "bcrypt$16384$8$1$" + saltHex + "$" + keyHex)).isFalse();
        assertThat(passwords.verify(PASSWORD, "scrypt$16384$8$1$zz" + saltHex.substring(2) + "$" + keyHex)).isFalse();
        assertThat(passwords.verify(PASSWORD, "scrypt$abc$8$1$" + saltHex + "$" + keyHex)).isFalse();
    }

    @Test
    void verify_refuses_a_stored_value_without_a_key() {
        assertThat(passwords.verify(PASSWORD, "scrypt$16384$8$1$" + HexFormat.of().formatHex(SALT) + "$")).isFalse();
    }

    /**
     * A stored value in the D1 format with the given parameters, whose key really is the scrypt key of {@code
     * PASSWORD}: before a bound existed, a value just outside the bounds verified, so a refusal below proves the bound.
     */
    private static String stored(int n, int r, int p, int saltBytes, int keyBytes) {
        byte[] salt = new byte[saltBytes];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) (i + 1);
        }
        byte[] key = SCrypt.generate(PASSWORD.getBytes(StandardCharsets.UTF_8), salt, n, r, p, keyBytes);
        HexFormat hex = HexFormat.of();
        return String.join("$", "scrypt", Integer.toString(n), Integer.toString(r), Integer.toString(p),
                hex.formatHex(salt), hex.formatHex(key));
    }

    /** A stored value with parameters that scrypt itself cannot run, so its key is made up. */
    private static String storedWithoutRealKey(String n, String r, String p) {
        return String.join("$", "scrypt", n, r, p, HexFormat.of().formatHex(SALT), "00".repeat(32));
    }

    @Test
    void the_d1_value_still_verifies() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 32))).isTrue();
    }

    @Test
    void verify_accepts_the_largest_cost_65536() {
        assertThat(passwords.verify(PASSWORD, stored(65536, 8, 1, 16, 32))).isTrue();
    }

    @Test
    void verify_refuses_a_cost_above_65536() {
        assertThat(passwords.verify(PASSWORD, stored(131072, 8, 1, 16, 32))).isFalse();
    }

    @Test
    void verify_does_no_scrypt_work_for_an_absurd_cost() {
        // 2^22 at r=8 would need gigabytes; the answer must come before any memory is claimed.
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("4194304", "8", "1"))).isFalse();
            assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("1073741824", "8", "1"))).isFalse();
        });
    }

    @Test
    void verify_accepts_the_smallest_cost_two() {
        assertThat(passwords.verify(PASSWORD, stored(2, 8, 1, 16, 32))).isTrue();
    }

    @Test
    void verify_refuses_a_cost_below_two_or_that_is_no_power_of_two() {
        for (String n : List.of("0", "1", "-2", "-16384", "3", "16383", "24576")) {
            assertThat(passwords.verify(PASSWORD, storedWithoutRealKey(n, "8", "1"))).as("N=" + n).isFalse();
        }
    }

    @Test
    void verify_accepts_the_largest_block_size_16() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 16, 1, 16, 32))).isTrue();
    }

    @Test
    void verify_refuses_a_block_size_above_16() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 17, 1, 16, 32))).isFalse();
        assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("16384", "100000", "1"))).isFalse());
    }

    @Test
    void verify_refuses_a_block_size_below_one() {
        for (String r : List.of("0", "-1")) {
            assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("16384", r, "1"))).as("r=" + r).isFalse();
        }
    }

    @Test
    void verify_accepts_the_largest_parallelism_4() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 4, 16, 32))).isTrue();
    }

    @Test
    void verify_refuses_a_parallelism_above_4() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 5, 16, 32))).isFalse();
        assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("16384", "8", "262143"))).isFalse());
    }

    @Test
    void verify_refuses_a_parallelism_below_one() {
        for (String p : List.of("0", "-1")) {
            assertThat(passwords.verify(PASSWORD, storedWithoutRealKey("16384", "8", p))).as("p=" + p).isFalse();
        }
    }

    @Test
    void verify_refuses_a_salt_shorter_than_eight_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 0, 32))).as("empty salt").isFalse();
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 7, 32))).as("7 bytes").isFalse();
    }

    @Test
    void verify_accepts_salts_of_eight_to_sixty_four_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 8, 32))).as("8 bytes").isTrue();
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 64, 32))).as("64 bytes").isTrue();
    }

    @Test
    void verify_refuses_a_salt_longer_than_sixty_four_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 65, 32))).isFalse();
    }

    @Test
    void verify_refuses_a_key_shorter_than_sixteen_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 15))).isFalse();
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 1))).isFalse();
    }

    @Test
    void verify_accepts_keys_of_sixteen_to_sixty_four_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 16))).as("16 bytes").isTrue();
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 64))).as("64 bytes").isTrue();
    }

    @Test
    void verify_refuses_a_key_longer_than_sixty_four_bytes() {
        assertThat(passwords.verify(PASSWORD, stored(16384, 8, 1, 16, 65))).isFalse();
    }

    @Test
    void two_hashes_of_one_password_differ_with_different_salts() {
        byte[] other = HexFormat.of().parseHex("ffeeddccbbaa99887766554433221100");

        String first = passwords.hash(PASSWORD);
        String second = new Passwords(new RecordingTokenGenerator(other)).hash(PASSWORD);

        assertThat(first).isNotEqualTo(second);
        assertThat(first.split("[$]")[5]).isNotEqualTo(second.split("[$]")[5]);
    }

    @Test
    void hash_output_contains_no_password() {
        assertThat(passwords.hash(PASSWORD)).doesNotContain(PASSWORD);
    }
}
