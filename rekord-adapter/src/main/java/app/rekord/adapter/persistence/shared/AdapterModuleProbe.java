package app.rekord.adapter.persistence.shared;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Exists to prove Jandex bean discovery from a library module (PIN-AC-0153). May be removed once the module has a
 * real bean and a test that injects it.
 */
@ApplicationScoped
public class AdapterModuleProbe {

    public String module() {
        return "rekord-adapter";
    }
}
