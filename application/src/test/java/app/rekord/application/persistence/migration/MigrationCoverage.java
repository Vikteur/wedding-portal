package app.rekord.application.persistence.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Matches the versioned migrations of a folder with their {@code V<version>MigrationIT} classes. Dots and underscores in a
 * version are the same to Flyway, so {@code V1_1__a.sql} and {@code V1.1__a.sql} are both version {@code 1.1}, tested by
 * {@code V1_1MigrationIT}. {@link #versionOf} is the one place that says what a versioned migration file name is; any
 * other test that has to agree with the gate calls it instead of copying the pattern.
 */
public final class MigrationCoverage {

    private static final Pattern MIGRATION_FILE = Pattern.compile("V([0-9]+(?:[._][0-9]+)*)__.+[.]sql");
    private static final Pattern TEST_CLASS = Pattern.compile("V([0-9]+(?:_[0-9]+)*)MigrationIT");
    private static final String PACKAGE = MigrationSchemaCheck.class.getPackageName();
    private static final String GATED_FOLDER = "application/src/main/resources/db/migration";
    private static final Pattern SOURCE_MIGRATION_FOLDER = Pattern.compile("(?:.+/)?src/[^/]+/(?:resources|java)/db/migration");
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(".git", ".gradle", "build", "node_modules", "contract");

    private MigrationCoverage() {}

    /**
     * The versions of the versioned migrations directly in the folder, in ascending order. A V file in a subfolder is not
     * listed, since no test class could be matched with it: {@link #unrecognised} reports the subfolder instead.
     */
    static List<String> versions(Path folder) {
        return files(folder).stream()
                .map(MigrationCoverage::versionOf)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(MigrationVersion::fromVersion))
                .toList();
    }

    /**
     * The version of a versioned migration file name, with dots (so {@code V1_1__x.sql} and {@code V1.1__x.sql} are
     * both {@code 1.1}), or empty when the name is no versioned migration: {@code V<version>__<description>.sql}, any
     * description.
     */
    public static Optional<String> versionOf(String fileName) {
        Matcher m = MIGRATION_FILE.matcher(fileName);
        return m.matches() ? Optional.of(m.group(1).replace('_', '.')) : Optional.empty();
    }

    static String testClassName(String version) {
        return PACKAGE + ".V" + version.replace('.', '_') + "MigrationIT";
    }

    /**
     * The versions without a concrete, enabled test class of the right version, each with the reason, such as
     * {@code "2: V2MigrationIT is @Disabled"}.
     */
    static List<String> untested(Path folder, Function<String, Optional<Class<?>>> lookup) {
        return versions(folder).stream()
                .flatMap(v -> problem(v, lookup.apply(v)).stream())
                .toList();
    }

    /** The {@code V*MigrationIT} classes of the package whose version has no V file. */
    static List<String> orphans(Path folder) {
        try {
            Path classes = Path.of(MigrationSchemaCheck.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .resolve(PACKAGE.replace('.', '/'));
            try (Stream<Path> list = Files.list(classes)) {
                List<String> names = list.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".class"))
                        .map(n -> PACKAGE + "." + n.substring(0, n.length() - ".class".length()))
                        .toList();
                return orphans(folder, names);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> orphans(Path folder, List<String> classNames) {
        List<String> versions = versions(folder);
        return classNames.stream()
                .filter(name -> {
                    Matcher m = TEST_CLASS.matcher(name.substring(name.lastIndexOf('.') + 1));
                    return m.matches() && !versions.contains(m.group(1).replace('_', '.'));
                })
                .sorted()
                .toList();
    }

    /**
     * The {@code src/<set>/resources/db/migration} folders of the repository other than the application's main one, and
     * every {@code src/<set>/java/db/migration} folder, where Flyway finds Java-based migrations. Every module's resources
     * reach {@code classpath:db/migration}, so a V file there would escape the gate, and so would a migration class.
     */
    static List<String> strayMigrationFolders(Path repoRoot) {
        List<String> stray = new ArrayList<>();
        try {
            Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    if (!dir.equals(repoRoot) && SKIPPED_DIRECTORIES.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String relative = repoRoot.relativize(dir).toString().replace('\\', '/');
                    if (SOURCE_MIGRATION_FOLDER.matcher(relative).matches() && !relative.equals(GATED_FOLDER)) {
                        stray.add(relative);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stray.stream().sorted().toList();
    }

    /**
     * The entries of the folder that Flyway would not run as a top-level versioned migration: files whose name is not
     * {@code V<version>__<description>.sql}, and every subfolder, written with a trailing {@code /}. Quarkus and Flyway
     * both walk into a subfolder, so a V file there would run at boot without a {@code V<version>MigrationIT}.
     */
    static List<String> unrecognised(Path folder) {
        try (Stream<Path> list = Files.list(folder)) {
            return list.filter(p -> !Files.isRegularFile(p) || !MIGRATION_FILE.matcher(p.getFileName().toString()).matches())
                    .map(p -> p.getFileName() + (Files.isDirectory(p) ? "/" : ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Why the candidate does not count as the test of the version, as {@code "<version>: <class> <reason>"}, or empty when
     * it does. A class that cannot be instantiated is not "untested" but broken: its own exception is rethrown.
     */
    private static Optional<String> problem(String version, Optional<Class<?>> candidate) {
        if (candidate.isEmpty()) {
            return Optional.of(version + ": " + expectedName(version) + " does not exist");
        }
        Class<?> type = candidate.get();
        String prefix = version + ": " + type.getSimpleName();
        if (!MigrationSchemaCheck.class.isAssignableFrom(type)) {
            return Optional.of(prefix + " does not extend MigrationSchemaCheck");
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            return Optional.of(prefix + " is abstract");
        }
        if (type.isAnnotationPresent(Disabled.class)) {
            return Optional.of(prefix + " is @Disabled");
        }
        Optional<String> condition = executionCondition(type);
        if (condition.isPresent()) {
            return Optional.of(prefix + " carries @" + condition.get() + ", a JUnit condition that can skip the check");
        }
        if (overridesTheCheck(type)) {
            return Optional.of(prefix + " overrides the inherited check");
        }
        String declared = declaredVersion(type);
        if (!version.equals(declared)) {
            return Optional.of(prefix + " declares version \"" + declared + "\" instead of \"" + version + "\"");
        }
        return Optional.empty();
    }

    private static String expectedName(String version) {
        String name = testClassName(version);
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static String declaredVersion(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return ((MigrationSchemaCheck) constructor.newInstance()).version();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(type.getSimpleName() + " throws from its constructor", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getSimpleName() + " cannot be instantiated to read its version()", e);
        }
    }

    /** A JUnit condition such as {@code @EnabledIfSystemProperty} could skip the check; the simple name of the first one. */
    private static Optional<String> executionCondition(Class<?> type) {
        return Arrays.stream(type.getAnnotations())
                .map(Annotation::annotationType)
                .filter(a -> a.getPackageName().equals(Disabled.class.getPackageName() + ".condition"))
                .map(Class::getSimpleName)
                .findFirst();
    }

    /** A subclass that redeclares the inherited test replaces it, with or without {@code @Test}. */
    private static boolean overridesTheCheck(Class<?> type) {
        List<String> checks = Arrays.stream(MigrationSchemaCheck.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class))
                .map(Method::getName)
                .toList();
        for (Class<?> c = type; c != MigrationSchemaCheck.class; c = c.getSuperclass()) {
            if (Arrays.stream(c.getDeclaredMethods()).anyMatch(m -> m.getParameterCount() == 0 && checks.contains(m.getName()))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> files(Path folder) {
        try (Stream<Path> list = Files.list(folder)) {
            return list.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
