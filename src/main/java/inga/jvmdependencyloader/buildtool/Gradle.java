package inga.jvmdependencyloader.buildtool;

import inga.jvmdependencyloader.Artifact;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Gradle implements BuildTool {
    private final Path gradleHome;
    private final Path subProjectPath;
    private final Path rootProjectPath;
    private final Pattern artifactPattern = Pattern.compile("([\\w\\.-]+):([\\w\\.-]+)(?::| -> )([\\w\\.]+)");

    public Gradle(Path subProjectPath, Path rootProjectPath) {
        gradleHome = Paths.get(System.getProperty("user.home")).resolve(".gradle");
        this.subProjectPath = subProjectPath;
        this.rootProjectPath = rootProjectPath;
    }

    @Override
    public URLClassLoader load() {
        return new URLClassLoader(findArtifacts().stream()
                .map(this::findJarUrl)
                .filter(Objects::nonNull)
                .toArray(URL[]::new));
    }

    @Override
    public List<Path> findCompiledClassPaths() {
        return List.of(
                subProjectPath.resolve("build/classes/java/main"),
                subProjectPath.resolve("build/classes/kotlin/main")
        );
    }

    private List<Artifact> findArtifacts() {
        if (!Files.exists(rootProjectPath.resolve("gradlew"))) {
            System.err.println("gradlew is not found in " + rootProjectPath);
            return Collections.emptyList();
        }
        try {
            var process = new ProcessBuilder(
                    "./gradlew", "-q", "dependencies", "--configuration", "compileClasspath")
                    .directory(rootProjectPath.toFile())
                    .start();
            var exitCode = process.waitFor();
            if (exitCode != 0) {
                try (var reader = process.errorReader()) {
                    System.err.println(reader.lines().collect(Collectors.joining(System.lineSeparator())));
                }
                return Collections.emptyList();
            }
            try (var reader = process.inputReader()) {
                var results = new ArrayList<Artifact>();
                for (var line : reader.lines().toList()) {
                    var matcher = artifactPattern.matcher(line);
                    if (matcher.find()) {
                        results.add(new Artifact(matcher.group(1), matcher.group(2), matcher.group(3)));
                    }
                }
                return results.stream().distinct().toList();
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL findJarUrl(Artifact artifact) {
        var caches = System.getenv("GRADLE_RO_DEP_CACHE") == null
                ? gradleHome.resolve("caches")
                : Path.of(System.getenv("GRADLE_RO_DEP_CACHE"));
        var pattern = "glob:"
                + String.join(
                File.separator,
                caches.toString(),
                "modules-*",
                "files-*",
                artifact.groupId(),
                artifact.artifactId(),
                artifact.version(),
                "*",
                artifact.artifactId() + '-' + artifact.version() + ".jar");
        try (var stream = Files.walk(caches, 7)) {
            var matcher = FileSystems.getDefault().getPathMatcher(pattern);
            var path = stream.filter(matcher::matches).findFirst().orElse(null);
            return path == null ? null : path.toUri().toURL();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
