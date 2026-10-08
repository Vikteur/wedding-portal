package app.rekord.usecase.identity;

import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import app.rekord.usecase.identity.port.PasswordHasher;
import app.rekord.usecase.shared.port.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Creates the first business and its admin when the settings name one and no active admin exists yet. */
@ApplicationScoped
public class BootstrapFirstAdminUseCase {

    /** Counted as UTF-16 units ({@code String.length()}), as rekord-api's {@code Bootstrap} does: six emoji pass. */
    static final int MIN_PASSWORD_LENGTH = 12;
    private static final String TIMEZONE = "Europe/Amsterdam";

    private final FirstAdminRepository repository;
    private final PasswordHasher hasher;
    private final IdGenerator ids;
    private final Clock clock;

    public BootstrapFirstAdminUseCase(
            FirstAdminRepository repository, PasswordHasher hasher, IdGenerator ids, Clock clock) {
        this.repository = repository;
        this.hasher = hasher;
        this.ids = ids;
        this.clock = clock;
    }

    @Transactional
    public BootstrapOutcome execute(BootstrapFirstAdminCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            return new BootstrapOutcome.NotConfigured();
        }
        if (repository.hasActiveAdmin()) {
            return new BootstrapOutcome.AdminExists();
        }
        if (command.password().length() < MIN_PASSWORD_LENGTH) {
            return new BootstrapOutcome.PasswordTooShort();
        }
        Instant now = clock.instant();
        UUID businessId = ids.newId();
        UUID accountId = ids.newId();
        UUID membershipId = ids.newId();
        repository.saveFirstAdmin(new NewFirstAdmin(
                businessId,
                command.businessName(),
                slugify(command.businessName()),
                TIMEZONE,
                accountId,
                command.email().strip().toLowerCase(Locale.ROOT),
                command.displayName(),
                hasher.hash(command.password()),
                membershipId,
                "ACTIVE",
                "ADMIN",
                "ACTIVE",
                now));
        return new BootstrapOutcome.Created(accountId, businessId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** ASCII only, as rekord-api's {@code Bootstrap.slugify}: any other character, accents too, is a dash. */
    private static String slugify(String name) {
        String slug = (name == null ? "" : name)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "org" : slug;
    }
}
