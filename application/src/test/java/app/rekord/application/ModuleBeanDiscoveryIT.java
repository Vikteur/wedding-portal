package app.rekord.application;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.shared.AdapterModuleProbe;
import app.rekord.gateway.shared.GatewayModuleProbe;
import app.rekord.usecase.shared.UseCaseModuleProbe;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ModuleBeanDiscoveryIT {

    @Inject
    UseCaseModuleProbe useCaseProbe;

    @Inject
    AdapterModuleProbe adapterProbe;

    @Inject
    GatewayModuleProbe gatewayProbe;

    @Test
    void injects_one_bean_from_each_library_module() {
        assertThat(useCaseProbe.module()).isEqualTo("rekord-usecase");
        assertThat(adapterProbe.module()).isEqualTo("rekord-adapter");
        assertThat(gatewayProbe.module()).isEqualTo("rekord-gateway");
    }

    @Test
    void each_library_module_ships_its_own_jandex_index() throws Exception {
        for (Class<?> probe : List.of(UseCaseModuleProbe.class, AdapterModuleProbe.class, GatewayModuleProbe.class)) {
            assertThat(hasJandexIndex(probe)).as(probe.getName()).isTrue();
        }
    }

    private static boolean hasJandexIndex(Class<?> type) throws URISyntaxException, IOException {
        Path location = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(location)) {
            return Files.exists(location.resolve("META-INF/jandex.idx"));
        }
        try (ZipFile jar = new ZipFile(location.toFile())) {
            return jar.getEntry("META-INF/jandex.idx") != null;
        }
    }
}
