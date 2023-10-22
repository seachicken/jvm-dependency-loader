package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Gradle implements BuildTool {
    private final Path root;

    public Gradle(Path root) {
        this.root = root;
    }

    @Override
    public URLClassLoader load() {
        backupBuildGradle();
        copyDependencies();
        var classLoader = new URLClassLoader(findJarUrls(root.resolve("target/dependency")));
        restoreBuildGradle();
        return classLoader;
    }

    @Override
    public Path findCompiledClassPath() {
        return root.resolve("build/classes/java/main");
    }

    private void copyDependencies() {
        var gradleStream = getClass().getClassLoader().getResourceAsStream("ingaCopyDependencies.gradle");
        try {
            Files.copy(gradleStream, root.resolve("ingaCopyDependencies.gradle"), StandardCopyOption.REPLACE_EXISTING);
            var process = new ProcessBuilder("bash", "-c",
                    "echo apply \"from: 'ingaCopyDependencies.gradle'\" >> build.gradle && "
                            + "./gradlew ingaCopyDependencies -q")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void backupBuildGradle() {
        try {
            var process = new ProcessBuilder("bash", "-c",
                    "cp build.gradle build.gradle.org")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void restoreBuildGradle() {
        try {
            var process = new ProcessBuilder("bash", "-c",
                    "mv build.gradle.org build.gradle && rm ingaCopyDependencies.gradle")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
