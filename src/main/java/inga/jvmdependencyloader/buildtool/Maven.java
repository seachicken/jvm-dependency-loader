package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Maven implements BuildTool {
    private final Path root;

    public Maven(Path root) {
        this.root = root;
    }

    @Override
    public URLClassLoader load() {
        return new URLClassLoader(getJarUrls());
    }

    @Override
    public Path findCompiledClassPath() {
        return root.resolve("target/classes");
    }

    private URL[] getJarUrls() {
        try {
            var process = new ProcessBuilder(
                    "mvn",
                    "dependency:build-classpath",
                    "-DincludeScope=compile",
                    "-Dmdep.outputFile=/dev/stdout",
                    "-q")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
            try (var reader = process.inputReader()) {
                var results = new ArrayList<>();
                for (var path : reader.lines().flatMap(l -> Arrays.stream(l.split(":"))).toList()) {
                    results.add(Path.of(path).toUri().toURL());
                }
                return results.toArray(URL[]::new);
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
