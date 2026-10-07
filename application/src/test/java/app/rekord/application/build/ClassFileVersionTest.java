package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClassFileVersionTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final int JAVA_25_MAJOR_VERSION = 69;
    private static final List<String> MODULES_WITH_CLASSES =
            List.of("rekord-usecase", "rekord-adapter", "rekord-gateway", "application");

    @Test
    void every_compiled_class_has_major_version_69() throws IOException {
        Map<String, Integer> classesPerModule = new TreeMap<>();
        List<String> wrongVersions = new ArrayList<>();

        for (String module : GradleSettings.includedModules(REPO_ROOT)) {
            int count = 0;
            for (String sourceSet : List.of("main", "test")) {
                Path classesDir = REPO_ROOT.resolve(module).resolve("build/classes/java").resolve(sourceSet);
                if (!Files.isDirectory(classesDir)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(classesDir)) {
                    for (Path classFile : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                        count++;
                        int major = majorVersion(classFile);
                        if (major != JAVA_25_MAJOR_VERSION) {
                            wrongVersions.add(REPO_ROOT.relativize(classFile) + " has major version " + major);
                        }
                    }
                }
            }
            classesPerModule.put(module, count);
        }

        assertThat(wrongVersions).isEmpty();
        for (String module : MODULES_WITH_CLASSES) {
            assertThat(classesPerModule.get(module)).as("compiled classes in %s", module).isPositive();
        }
    }

    private static int majorVersion(Path classFile) throws IOException {
        try (InputStream in = Files.newInputStream(classFile);
                DataInputStream data = new DataInputStream(in)) {
            data.skipBytes(6);
            return data.readUnsignedShort();
        }
    }
}
