package app.rekord.application.persistence.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;

/**
 * Matches the versioned migrations of a folder with their {@code V<version>MigrationIT} classes. Dots and underscores in a
 * version are the same to Flyway, so {@code V1_1__a.sql} and {@code V1.1__a.sql} are both version {@code 1.1}, tested by
 * {@code V1_1MigrationIT}.
 */
final class MigrationCoverage {

    private static final Pattern MIGRATION_FILE = Pattern.compile("V([0-9]+(?:[._][0-9]+)*)__.+[.]sql");
    private static final Pattern TEST_CLASS = Pattern.compile("V([0-9]+(?:_[0-9]+)*)MigrationIT");
    private static final String PACKAGE = MigrationSchemaCheck.class.getPackageName();

    private MigrationCoverage() {}

    /** The versions of the versioned migrations in the folder, in ascending order. */
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

    /** The files of the folder that Flyway would not run as a versioned migration. */
    static List<String> unrecognised(Path folder) {
        return files(folder).stream().filter(n -> !MIGRATION_FILE.matcher(n).matches()).sorted().toList();
    }

    private static boolean isTested(String version, Optional<Class<?>> candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        Class<?> type = candidate.get();
        if (!MigrationSchemaCheck.class.isAssignableFrom(type)
                || Modifier.isAbstract(type.getModifiers())
                || type.isAnnotationPresent(Disabled.class)) {
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
