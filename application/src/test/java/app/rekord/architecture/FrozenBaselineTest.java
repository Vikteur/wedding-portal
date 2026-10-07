package app.rekord.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore;
import com.tngtech.archunit.library.freeze.ViolationStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrozenBaselineTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path STORE = REPO_ROOT.resolve("application/src/test/archunit_store");
    private static final Path PROPERTIES = REPO_ROOT.resolve("application/src/test/resources/archunit.properties");
    private static final List<Class<?>> SUITE = List.of(
            DomainArchitectureTest.class,
            LayerArchitectureTest.class,
            UseCaseArchitectureTest.class,
            AdapterArchitectureTest.class,
            ProductionCodeArchitectureTest.class);

    @TempDir
    Path temp;

    @Test
    void archunit_properties_forbid_creating_and_updating_the_store() throws IOException {
        assertThat(PROPERTIES).isRegularFile();
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(PROPERTIES)) {
            properties.load(in);
        }

        assertThat(properties.getProperty("freeze.store.default.path")).isEqualTo("src/test/archunit_store");
        assertThat(properties.getProperty("freeze.store.default.allowStoreCreation")).isEqualTo("false");
        assertThat(properties.getProperty("freeze.store.default.allowStoreUpdate")).isEqualTo("false");
        assertThat(properties.getProperty("freeze.refreeze", "false")).isEqualTo("false");
    }

    @Test
    void the_store_holds_no_frozen_violation() throws IOException {
        assertThat(STORE.resolve("stored.rules")).isRegularFile();
        try (Stream<Path> files = Files.list(STORE)) {
            List<Path> violationFiles = files.filter(f -> !f.getFileName().toString().equals("stored.rules")).toList();

            assertThat(violationFiles).isNotEmpty();
            for (Path file : violationFiles) {
                assertThat(Files.readString(file)).as("frozen violations in %s", file.getFileName()).isBlank();
            }
        }
    }

    @Test
    void the_store_knows_exactly_the_rules_of_the_suite() throws IOException {
        Properties stored = new Properties();
        try (InputStream in = Files.newInputStream(STORE.resolve("stored.rules"))) {
            stored.load(in);
        }
        Set<String> descriptions = new TreeSet<>();
        for (Class<?> suiteClass : SUITE) {
            for (Field field : suiteClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ArchTest.class)) {
                    descriptions.add(rule(field).getDescription().replace("\r\n", "\n"));
                }
            }
        }

        assertThat(stored.stringPropertyNames()).isEqualTo(descriptions);
    }

    @Test
    void a_new_violation_of_a_stored_rule_fails_and_leaves_the_store_unchanged() throws IOException {
        Path copy = copyOfTheStore();
        Map<String, byte[]> before = snapshot(copy);
        JavaClasses violating = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                import jakarta.inject.Inject;

                public class InjectedThing {
                    @Inject
                    String name;
                }
                """);

        FreezingArchRule frozen = FreezingArchRule.freeze(DomainRules.A1).persistIn(storeOver(copy));

        assertThatThrownBy(() -> frozen.check(violating))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("app.rekord.domain.fixture.InjectedThing");
        assertThat(snapshot(copy)).containsOnlyKeys(before.keySet());
        before.forEach((name, bytes) -> assertThat(snapshot(copy).get(name)).as(name).isEqualTo(bytes));
    }

    @Test
    void a_rule_missing_from_the_store_fails_instead_of_being_frozen() throws IOException {
        Path copy = copyOfTheStore();
        Map<String, byte[]> before = snapshot(copy);
        JavaClasses violating = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                public class FrameworkUser {
                    jakarta.inject.Provider<String> provider;
                }
                """);
        ArchRule unknown = noClasses()
                .that()
                .resideInAPackage("app.rekord.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("jakarta..")
                .because("ZZ99 (not in the store): a rule nobody froze");

        FreezingArchRule frozen = FreezingArchRule.freeze(unknown).persistIn(storeOver(copy));

        assertThatThrownBy(() -> frozen.check(violating)).isInstanceOf(Throwable.class);
        assertThat(snapshot(copy)).containsOnlyKeys(before.keySet());
        before.forEach((name, bytes) -> assertThat(snapshot(copy).get(name)).as(name).isEqualTo(bytes));
    }

    private Path copyOfTheStore() throws IOException {
        Path copy = Files.createDirectories(temp.resolve("store-copy"));
        try (Stream<Path> files = Files.list(STORE)) {
            for (Path file : files.toList()) {
                Files.copy(file, copy.resolve(file.getFileName()));
            }
        }
        return copy;
    }

    private static Map<String, byte[]> snapshot(Path directory) {
        Map<String, byte[]> contents = new TreeMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                contents.put(file.getFileName().toString(), Files.readAllBytes(file));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return contents;
    }

    /** FreezingArchRule re-initialises its store from the global configuration, so the copy's settings are pinned here. */
    private static ViolationStore storeOver(Path directory) {
        Properties pinned = new Properties();
        pinned.setProperty("default.path", directory.toString());
        pinned.setProperty("default.allowStoreCreation", "false");
        pinned.setProperty("default.allowStoreUpdate", "false");
        TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();
        return new ViolationStore() {
            @Override
            public void initialize(Properties ignored) {
                delegate.initialize(pinned);
            }

            @Override
            public boolean contains(ArchRule rule) {
                return delegate.contains(rule);
            }

            @Override
            public void save(ArchRule rule, List<String> violations) {
                delegate.save(rule, violations);
            }

            @Override
            public List<String> getViolations(ArchRule rule) {
                return delegate.getViolations(rule);
            }
        };
    }

    private static ArchRule rule(Field field) {
        try {
            field.setAccessible(true);
            return (ArchRule) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
