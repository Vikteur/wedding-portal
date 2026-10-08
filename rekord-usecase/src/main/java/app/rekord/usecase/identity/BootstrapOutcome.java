package app.rekord.usecase.identity;

import java.util.UUID;

/** What the bootstrap did; the caller decides what to log. */
public sealed interface BootstrapOutcome {

    record Created(UUID accountId, UUID businessId) implements BootstrapOutcome {
    }

    /** No address or no password is set. */
    record NotConfigured() implements BootstrapOutcome {
    }

    /** An active admin already exists. */
    record AdminExists() implements BootstrapOutcome {
    }

    /** The password has fewer than 12 UTF-16 units ({@code String.length()}), counted as rekord-api does. */
    record PasswordTooShort() implements BootstrapOutcome {
    }
}
