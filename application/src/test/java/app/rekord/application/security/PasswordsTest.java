package app.rekord.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.usecase.shared.port.TokenGenerator;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
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
        assertThat(passwords.hash(PASSWORD)).matches("^scrypt\$16384\$8\$1\$[0-9a-f]{32}\$[0-9a-f]{64}$");
    }

    @Test
    void the_salt_is_sixteen_bytes_from_the_token_generator() {
        String hash = passwords.hash(PASSWORD);

        assertThat(tokens.asked).containsExactly(16);
        assertThat(hash.split("\$")[4]).isEqualTo(HexFormat.of().formatHex(SALT));
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
        String keyHex = good.split("\$")[5];

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
    void two_hashes_of_one_password_differ_with_different_salts() {
        byte[] other = HexFormat.of().parseHex("ffeeddccbbaa99887766554433221100");

        String first = passwords.hash(PASSWORD);
        String second = new Passwords(new RecordingTokenGenerator(other)).hash(PASSWORD);

        assertThat(first).isNotEqualTo(second);
        assertThat(first.split("\$")[5]).isNotEqualTo(second.split("\$")[5]);
    }

    @Test
    void hash_output_contains_no_password() {
        assertThat(passwords.hash(PASSWORD)).doesNotContain(PASSWORD);
    }
}
