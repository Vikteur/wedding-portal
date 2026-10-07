package app.rekord.architecture;

import app.rekord.application.build.GradleSettings;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.LocationProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** The main class directories of every module in settings.gradle.kts; no test classes. */
public final class ProductionClasses implements LocationProvider {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    @Override
    public Set<Location> get(Class<?> testClass) {
        Set<Location> locations = new LinkedHashSet<>();
        try {
            for (String module : GradleSettings.includedModules(REPO_ROOT)) {
                locations.add(Location.of(REPO_ROOT.resolve(module).resolve("build/classes/java/main")));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return locations;
    }

    public static JavaClasses importAll() {
        return new ClassFileImporter().importLocations(new ProductionClasses().get(ProductionClasses.class));
    }
}
