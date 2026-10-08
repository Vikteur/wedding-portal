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
 * Creates the first admin once the application has started. A refusal is logged and the application still starts.
 * No line names an address, a name, a password or a hash: only the new ids and the reason.
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
            LOG.error("First admin not created: the address or the business name is already taken.");
            return;
        }
        switch (outcome) {
            case BootstrapOutcome.Created created -> LOG.infof(
                    "First admin created: business %s, account %s.", created.businessId(), created.accountId());
            case BootstrapOutcome.PasswordTooShort tooShort ->
                    LOG.error("First admin not created: the password must have at least 12 characters.");
            case BootstrapOutcome.NotConfigured notConfigured -> { }
            case BootstrapOutcome.AdminExists adminExists -> { }
        }
    }
}
