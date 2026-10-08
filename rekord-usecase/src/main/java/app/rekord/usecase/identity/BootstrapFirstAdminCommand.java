package app.rekord.usecase.identity;

/** The settings of the first admin; any value may be null when it is not configured. */
public record BootstrapFirstAdminCommand(String email, String password, String displayName, String businessName) {

    /** Prints no address, name or password. */
    @Override
    public String toString() {
        return "BootstrapFirstAdminCommand[redacted]";
    }
}
