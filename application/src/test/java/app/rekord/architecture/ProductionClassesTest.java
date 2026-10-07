package app.rekord.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.build.GradleSettings;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.Location;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductionClassesTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    @Test
    void the_locations_are_the_main_class_directories_of_every_module_in_settings() throws IOException {
        Set<Path> expected = new TreeSet<>();
        for (String module : GradleSettings.includedModules(REPO_ROOT)) {
            expected.add(REPO_ROOT.resolve(module).resolve("build/classes/java/main").toAbsolutePath().normalize());
        }

        Set<Path> actual = new ProductionClasses().get(ProductionClassesTest.class).stream()
                .map(Location::asURI)
                .map(Path::of)
                .map(p -> p.toAbsolutePath().normalize())
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(expected).hasSize(6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void every_class_file_compiled_at_release_25_is_imported() throws IOException {
        Set<String> imported = ProductionClasses.importAll().stream()
                .map(c -> c.getName())
                .collect(Collectors.toSet());

        Set<String> onDisk = new TreeSet<>();
        for (String module : GradleSettings.includedModules(REPO_ROOT)) {
            Path dir = REPO_ROOT.resolve(module).resolve("build/classes/java/main");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String name = dir.relativize(file).toString().replace(java.io.File.separatorChar, '/');
                    name = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    if (!name.equals("module-info") && !name.endsWith(".package-info")) {
                        onDisk.add(name);
                    }
                }
            }
        }

        assertThat(onDisk).isNotEmpty();
        assertThat(imported).containsAll(onDisk);
    }

    @Test
    void the_import_holds_a_class_of_every_layer_and_the_generated_contract() {
        JavaClasses classes = ProductionClasses.importAll();

        List<String> required = List.of(
                "app.rekord.domain.matching.Normalize",
                "app.rekord.usecase.shared.UseCaseModuleProbe",
                "app.rekord.adapter.web.health.HealthResource",
                "app.rekord.api.HealthApi",
                "app.rekord.gateway.shared.GatewayModuleProbe",
                "app.rekord.application.config.ApiApplication");
        for (String name : required) {
            assertThat(classes.contain(name)).as(name).isTrue();
        }
    }

    @Test
    void no_test_class_is_imported() {
        JavaClasses classes = ProductionClasses.importAll();

        assertThat(classes.contain("app.rekord.application.web.SuccessStatusProbeResource")).isFalse();
        assertThat(classes.contain("app.rekord.architecture.ProductionClassesTest")).isFalse();
        assertThat(classes.stream().map(c -> c.getSource().get().getUri().toString()))
                .noneMatch(uri -> uri.contains("build/classes/java/test"));
    }
}
