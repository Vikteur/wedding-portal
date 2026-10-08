package app.rekord.application.security;

import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.BootstrapFirstAdminCommand;
import app.rekord.usecase.identity.BootstrapFirstAdminUseCase;
import app.rekord.usecase.identity.BootstrapOutcome;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Creates the first admin once the application has started. A refusal (the password is too short, the address or the
 * business name is taken, or a unique violation the database does not name) is logged and the application still
 * starts. Any other failure is not caught and stops the start, on purpose: the database being down, or a bug, must
 * not look like a healthy start without the admin the deployment asked for, and a "catch everything" here would hide
 * it. That exception is the original one, with its causes; it holds no refused value only because the datasource sets
 * {@code logServerErrorDetail=false} (docs/memory.md, TASK-7.3).
 *
 * <p>No line names an address, a name, a password or a hash: only the new ids and the reason. The bootstrap is meant
 * for the first start of a single instance: the check for an admin and the creation are not one lock, so two
 * instances starting at the same moment can both pass the check.
 */
@ApplicationScoped
public class FirstAdminBootstrap {

    private static final Logger LOG = Logger.getLogger(FirstAdminBootstrap.class);

    private final BootstrapSettings settings;
    private final BootstrapFirstAdminUseCase useCase;

    public FirstAdminBootstrap(BootstrapSettings settings, BootstrapFirstAdminUseCase useCase) {
        this.settings = settings;
        this.useCase = useCase;
    }

    private static boolean isBlank(Optional<String> value) {
        return value.isEmpty() || value.get().isBlank();
    }

    private static String refused(String reason) {
        return "Refusing to bootstrap the first admin: " + reason + ". No admin was created.";
    }

    void onStart(@Observes StartupEvent event) {
        if (isBlank(settings.email()) || isBlank(settings.password())) {
            // Not configured: the use case, and with it the datasource, is not touched (resource tests have none).
            return;
        }
        BootstrapOutcome outcome;
        try {
            outcome = useCase.execute(new BootstrapFirstAdminCommand(
                    settings.email().orElse(null),
                    settings.password().orElse(null),
                    settings.displayName(),
                    settings.orgName()));
        } catch (RejectedException e) {
            // Built from the refusal code alone: the refusal carries no cause, and its values are never read.
            LOG.error(refused(switch (e.code()) {
                case DUPLICATE_USERNAME -> "the bootstrap address (app.bootstrap.email) already belongs to an account";
                case DUPLICATE_NAME ->
                        "the business name (app.bootstrap.org-name) gives a slug another business already has";
                default -> "the database refused it (" + e.code() + ")";
            }));
            return;
        }
        switch (outcome) {
            case BootstrapOutcome.Created created -> LOG.infof(
                    "Created the first admin account %s in business %s", created.accountId(), created.businessId());
            case BootstrapOutcome.PasswordTooShort tooShort -> LOG.error(refused(
                    "the bootstrap password (app.bootstrap.password) needs at least 12 characters"));
            case BootstrapOutcome.NotConfigured notConfigured -> { }
            case BootstrapOutcome.AdminExists adminExists -> { }
        }
    }
}
