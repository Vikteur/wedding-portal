package app.rekord.usecase.identity;

/**
 * The settings of the first admin. The address or the password is null or blank when it is not configured; the display
 * name and the business name always have a value (the application sets a default for each).
 */
public record BootstrapFirstAdminCommand(String email, String password, String displayName, String businessName) {

    /** Prints no address, name or password. */
    @Override
    public String toString() {
        return "BootstrapFirstAdminCommand[redacted]";
    }
}
