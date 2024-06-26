package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public interface BuildTool {
    URLClassLoader load();
    Path findCompiledClassPath();

    static BuildTool create(Path root) {
        try (var stream = Files.walk(root)) {
            var fileNames = stream
                    .filter(p -> p.toFile().isFile())
                    .map(p -> p.getFileName().toString())
                    .toList();
            if (fileNames.contains("pom.xml")) {
                return new Maven(root);
            } else if (fileNames.contains("build.gradle")) {
                return new Gradle(root);
            } else if (fileNames.contains("build.gradle.kts")) {
                return new Gradle(root);
            }
            throw new IllegalArgumentException("no build tool found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
