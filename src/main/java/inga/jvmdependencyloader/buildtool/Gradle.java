package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Gradle implements BuildTool {
    @Override
    public URLClassLoader load(Path root) {
        backupBuildGradle(root);
        copyDependencies(root);
        var classLoader = new URLClassLoader(findJarUrls(root.resolve("target/dependency")));
        restoreBuildGradle(root);
        return classLoader;
    }

    private void copyDependencies(Path root) {
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

    private void backupBuildGradle(Path root) {
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

    private void restoreBuildGradle(Path root) {
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
