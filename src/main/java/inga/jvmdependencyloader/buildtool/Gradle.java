package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Gradle implements BuildTool {
    @Override
    public URLClassLoader load(Path root) {
        copyDependencies(root);
        return new URLClassLoader(findJarUrls(root.resolve("target/dependency")));
    }

    private void copyDependencies(Path root) {
        var gradleFile = Path.of(getClass().getClassLoader().getResource("ingaCopyDependencies.gradle").getFile());
        try {
            Files.copy(gradleFile, root.resolve("ingaCopyDependencies.gradle"), StandardCopyOption.REPLACE_EXISTING);
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
}
