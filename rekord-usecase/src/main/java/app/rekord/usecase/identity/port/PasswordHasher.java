package app.rekord.usecase.identity.port;

/** Turns a password into the string that is stored; the password itself is never stored. */
public interface PasswordHasher {

    String hash(String password);
}
