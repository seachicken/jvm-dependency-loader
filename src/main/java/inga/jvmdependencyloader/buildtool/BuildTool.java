package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BuildTool {
    URLClassLoader load();
    List<Path> findCompiledClassPaths();

    static BuildTool create(Path subProjectPath, Path rootProjectPath) {
        try (Stream<Path> stream = Files.list(subProjectPath)) {
            List<String> fileNames = stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            if (fileNames.contains("pom.xml")) {
                return new Maven(subProjectPath);
            } else if (fileNames.contains("build.gradle")) {
                return new Gradle(subProjectPath, rootProjectPath);
            } else if (fileNames.contains("build.gradle.kts")) {
                return new Gradle(subProjectPath, rootProjectPath);
            }
            throw new IllegalArgumentException("no build tool found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
