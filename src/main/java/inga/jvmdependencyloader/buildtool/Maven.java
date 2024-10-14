package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Maven implements BuildTool {
    private final Path root;

    public Maven(Path root) {
        this.root = root;
    }

    @Override
    public URLClassLoader load() {
        return new URLClassLoader(getJarUrls().toArray(URL[]::new));
    }

    @Override
    public Path findCompiledClassPath() {
        return root.resolve("target/classes");
    }

    private List<URL> getJarUrls() {
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
                var results = new ArrayList<URL>();
                for (var path : reader.lines().flatMap(l -> Arrays.stream(l.split(":"))).toList()) {
                    results.add(Path.of(path).toUri().toURL());
                }
                return results;
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
