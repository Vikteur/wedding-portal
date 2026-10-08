package app.rekord.application.security;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

/**
 * The settings of the first admin. The address and the password come from the deployment secrets
 * ({@code APP_BOOTSTRAP_EMAIL}, {@code APP_BOOTSTRAP_PASSWORD}) and are never set in a shipped file.
 */
@ConfigMapping(prefix = "app.bootstrap")
public interface BootstrapSettings {

    Optional<String> email();

    Optional<String> password();

    String displayName();

    String orgName();
}
