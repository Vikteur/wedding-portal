package app.rekord.usecase.identity;

import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import java.util.ArrayList;
import java.util.List;

/** Plain fake: no CDI annotation, so the Jandex-indexed test jar adds no bean to the application tests. */
final class InMemoryFirstAdminRepository implements FirstAdminRepository {

    boolean activeAdmin;
    int asked;
    final List<NewFirstAdmin> saved = new ArrayList<>();

    @Override
    public boolean hasActiveAdmin() {
        asked++;
        return activeAdmin;
    }

    @Override
    public void saveFirstAdmin(NewFirstAdmin admin) {
        saved.add(admin);
    }
}
