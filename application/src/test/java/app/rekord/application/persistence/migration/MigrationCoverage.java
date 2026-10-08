package app.rekord.application.persistence.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Matches the versioned migrations of a folder with their {@code V<version>MigrationIT} classes. Dots and underscores in a
 * version are the same to Flyway, so {@code V1_1__a.sql} and {@code V1.1__a.sql} are both version {@code 1.1}, tested by
 * {@code V1_1MigrationIT}.
 */
final class MigrationCoverage {

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
                .map(MIGRATION_FILE::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1).replace('_', '.'))
                .sorted(Comparator.comparing(MigrationCoverage::numericParts, MigrationCoverage::compareParts))
                .toList();
    }

    static String testClassName(String version) {
        return PACKAGE + ".V" + version.replace('.', '_') + "MigrationIT";
    }

    /** The versions without a concrete, enabled test class of the right version. */
    static List<String> untested(Path folder, Function<String, Optional<Class<?>>> lookup) {
        return versions(folder).stream().filter(v -> !isTested(v, lookup.apply(v))).toList();
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

    private static boolean isTested(String version, Optional<Class<?>> candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        Class<?> type = candidate.get();
        if (!MigrationSchemaCheck.class.isAssignableFrom(type)
                || Modifier.isAbstract(type.getModifiers())
                || type.isAnnotationPresent(Disabled.class)
                || hasExecutionCondition(type)
                || overridesTheCheck(type)) {
            return false;
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return version.equals(((MigrationSchemaCheck) constructor.newInstance()).version());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** A JUnit condition such as {@code @EnabledIfSystemProperty} could skip the check. */
    private static boolean hasExecutionCondition(Class<?> type) {
        return Arrays.stream(type.getAnnotations())
                .anyMatch(a -> a.annotationType().getPackageName().equals(Disabled.class.getPackageName() + ".condition"));
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

    private static int[] numericParts(String version) {
        return Arrays.stream(version.split("[.]")).mapToInt(Integer::parseInt).toArray();
    }

    private static int compareParts(int[] a, int[] b) {
        return Arrays.compare(a, b);
    }
}
