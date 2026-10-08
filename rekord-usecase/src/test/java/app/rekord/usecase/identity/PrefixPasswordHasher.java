package app.rekord.usecase.identity;

import app.rekord.usecase.identity.port.PasswordHasher;

final class PrefixPasswordHasher implements PasswordHasher {

    int calls;

    @Override
    public String hash(String password) {
        calls++;
        return "fake-hash:" + password.length();
    }
}
