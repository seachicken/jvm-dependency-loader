package inga.jvmdependencyloader.buildtool;

import inga.jvmdependencyloader.Artifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return Arrays.asList(
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
            Process process = new ProcessBuilder(
                    "./gradlew", "-q", "dependencies", "--configuration", "compileClasspath")
                    .directory(rootProjectPath.toFile())
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    System.err.println(reader.lines().collect(Collectors.joining(System.lineSeparator())));
                }
                return Collections.emptyList();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                ArrayList<Artifact> results = new ArrayList<>();
                for (String line : reader.lines().collect(Collectors.toList())) {
                    Matcher matcher = artifactPattern.matcher(line);
                    if (matcher.find()) {
                        results.add(new Artifact(matcher.group(1), matcher.group(2), matcher.group(3)));
                    }
                }
                return results.stream().distinct().collect(Collectors.toList());
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL findJarUrl(Artifact artifact) {
        Path caches = System.getenv("GRADLE_RO_DEP_CACHE") == null
                ? gradleHome.resolve("caches")
                : Paths.get(System.getenv("GRADLE_RO_DEP_CACHE"));
        String pattern = "glob:"
                + String.join(
                File.separator,
                caches.toString(),
                "modules-*",
                "files-*",
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                "*",
                artifact.getArtifactId() + '-' + artifact.getVersion() + ".jar");
        try (Stream<Path> stream = Files.walk(caches, 7)) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
            Path path = stream.filter(matcher::matches).findFirst().orElse(null);
            return path == null ? null : path.toUri().toURL();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
