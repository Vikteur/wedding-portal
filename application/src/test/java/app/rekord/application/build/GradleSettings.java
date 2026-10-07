package app.rekord.application.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradleSettings {

    private GradleSettings() {}

    public static Set<String> includedModules(Path repoRoot) throws IOException {
        String settings = Files.readString(repoRoot.resolve("settings.gradle.kts"));
        Set<String> included = new TreeSet<>();
        Matcher include = Pattern.compile("include\\(([^)]*)\\)").matcher(settings);
        while (include.find()) {
            Matcher name = Pattern.compile("\"([^\"]+)\"").matcher(include.group(1));
            while (name.find()) {
                included.add(name.group(1));
            }
        }
        return included;
    }
}
