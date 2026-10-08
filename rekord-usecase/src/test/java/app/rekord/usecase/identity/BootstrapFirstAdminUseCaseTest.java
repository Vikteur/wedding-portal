package app.rekord.usecase.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.usecase.identity.port.NewFirstAdmin;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BootstrapFirstAdminUseCaseTest {

    private static final Instant NOW = Instant.parse("2027-06-12T10:00:00Z");
    private static final String PASSWORD = "made-up-pass-1234";

    private final InMemoryFirstAdminRepository repository = new InMemoryFirstAdminRepository();
    private final PrefixPasswordHasher hasher = new PrefixPasswordHasher();
    private final BootstrapFirstAdminUseCase useCase = new BootstrapFirstAdminUseCase(
            repository, hasher, new SequentialIdGenerator(), Clock.fixed(NOW, ZoneOffset.UTC));

    private static BootstrapFirstAdminCommand command(String email, String password, String businessName) {
        return new BootstrapFirstAdminCommand(email, password, "The planner", businessName);
    }

    @Test
    void creates_the_business_the_admin_account_and_its_admin_membership() {
        BootstrapOutcome outcome = useCase.execute(command(" Admin@example.com ", PASSWORD, "Rekord Match"));

        assertThat(repository.saved).hasSize(1);
        NewFirstAdmin saved = repository.saved.get(0);
        assertThat(saved.businessName()).isEqualTo("Rekord Match");
        assertThat(saved.businessSlug()).isEqualTo("rekord-match");
        assertThat(saved.timezone()).isEqualTo("Europe/Amsterdam");
        assertThat(saved.email()).isEqualTo("admin@example.com");
        assertThat(saved.displayName()).isEqualTo("The planner");
        assertThat(saved.accountStatus()).isEqualTo("ACTIVE");
        assertThat(saved.role()).isEqualTo("ADMIN");
        assertThat(saved.membershipStatus()).isEqualTo("ACTIVE");
        assertThat(saved.passwordHash()).isEqualTo("fake-hash:" + PASSWORD.length());
        assertThat(saved.now()).isEqualTo(NOW);
        assertThat(saved.businessId()).isEqualTo(new UUID(0L, 1));
        assertThat(saved.accountId()).isEqualTo(new UUID(0L, 2));
        assertThat(saved.membershipId()).isEqualTo(new UUID(0L, 3));
        assertThat(outcome).isEqualTo(new BootstrapOutcome.Created(saved.accountId(), saved.businessId()));
    }

    @Test
    void the_slug_follows_rekord_api() {
        useCase.execute(command("a@example.com", PASSWORD, "Rekord Match"));
        useCase.execute(command("a@example.com", PASSWORD, "  Studio & Co!! "));
        useCase.execute(command("a@example.com", PASSWORD, "!!!"));

        assertThat(repository.saved)
                .extracting(NewFirstAdmin::businessSlug)
                .containsExactly("rekord-match", "studio-co", "org");
    }

    @Test
    void an_active_admin_means_nothing_is_created() {
        repository.activeAdmin = true;

        BootstrapOutcome outcome = useCase.execute(command("a@example.com", PASSWORD, "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.AdminExists.class);
        assertThat(repository.saved).isEmpty();
        assertThat(hasher.calls).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void a_missing_or_blank_email_creates_nothing(String email) {
        BootstrapOutcome outcome = useCase.execute(command(email, PASSWORD, "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.NotConfigured.class);
        assertThat(repository.asked).isZero();
        assertThat(repository.saved).isEmpty();
        assertThat(hasher.calls).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void a_missing_or_blank_password_creates_nothing(String password) {
        BootstrapOutcome outcome = useCase.execute(command("a@example.com", password, "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.NotConfigured.class);
        assertThat(repository.asked).isZero();
        assertThat(repository.saved).isEmpty();
        assertThat(hasher.calls).isZero();
    }

    @Test
    void a_password_of_11_characters_creates_nothing() {
        BootstrapOutcome outcome = useCase.execute(command("a@example.com", "12345678901", "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.PasswordTooShort.class);
        assertThat(repository.saved).isEmpty();
        assertThat(hasher.calls).isZero();
    }

    @Test
    void a_password_of_12_characters_is_accepted() {
        BootstrapOutcome outcome = useCase.execute(command("a@example.com", "123456789012", "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.Created.class);
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void the_password_length_counts_utf_16_units_as_rekord_api_does() {
        // Six emoji are 6 code points but 12 UTF-16 units: String.length() in the oracle lets them through.
        String sixEmoji = "😀".repeat(6);
        String fiveEmoji = "😀".repeat(5);

        assertThat(useCase.execute(command("a@example.com", fiveEmoji, "Rekord Match")))
                .isInstanceOf(BootstrapOutcome.PasswordTooShort.class);
        assertThat(useCase.execute(command("a@example.com", sixEmoji, "Rekord Match")))
                .isInstanceOf(BootstrapOutcome.Created.class);
    }

    @Test
    void the_slug_is_ascii_only_as_in_rekord_api() {
        // Every character outside a-z and 0-9, an accented letter included, becomes a dash: not a transliteration.
        useCase.execute(command("a@example.com", PASSWORD, "Café Noël"));

        assertThat(repository.saved).extracting(NewFirstAdmin::businessSlug).containsExactly("caf-no-l");
    }

    @Test
    void an_active_admin_is_checked_before_the_password_length() {
        repository.activeAdmin = true;

        BootstrapOutcome outcome = useCase.execute(command("a@example.com", "short", "Rekord Match"));

        assertThat(outcome).isInstanceOf(BootstrapOutcome.AdminExists.class);
    }

    @Test
    void the_command_and_the_new_rows_print_no_address_name_password_or_hash() {
        useCase.execute(command("admin@example.com", PASSWORD, "Rekord Match"));
        String printed = command("admin@example.com", PASSWORD, "Rekord Match") + " " + repository.saved.get(0);

        assertThat(printed)
                .doesNotContain("example.com")
                .doesNotContain("The planner")
                .doesNotContain("Rekord Match")
                .doesNotContain(PASSWORD)
                .doesNotContain("fake-hash");
    }
}
