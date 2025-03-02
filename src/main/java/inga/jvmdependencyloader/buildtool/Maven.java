package inga.jvmdependencyloader.buildtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Maven implements BuildTool {
    private final Path root;

    public Maven(Path root) {
        this.root = root;
    }

    @Override
    public URLClassLoader load() {
        return new URLClassLoader(getJarUrls().toArray(new URL[0]));
    }

    @Override
    public List<Path> findCompiledClassPaths() {
        return Collections.singletonList(root.resolve("target/classes"));
    }

    private List<URL> getJarUrls() {
        try {
            Process process = new ProcessBuilder(
                    "mvn",
                    "dependency:build-classpath",
                    "-DincludeScope=compile",
                    "-Dmdep.outputFile=/dev/stdout",
                    "-q")
                    .directory(root.toFile())
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    System.err.println(reader.lines().collect(Collectors.joining(System.lineSeparator())));
                }
                return Collections.emptyList();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                ArrayList<URL> results = new ArrayList<>();
                for (String path : reader.lines().flatMap(l -> Arrays.stream(l.split(":"))).collect(Collectors.toList())) {
                    results.add(Paths.get(path).toUri().toURL());
                }
                return results;
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
