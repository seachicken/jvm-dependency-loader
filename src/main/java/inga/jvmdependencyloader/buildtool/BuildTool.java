package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public interface BuildTool {
    URLClassLoader load(Path root);

    static BuildTool create(Path root) {
        try (var stream = Files.walk(root)) {
            var fileNames = stream
                    .filter(p -> p.toFile().isFile())
                    .map(p -> p.getFileName().toString())
                    .toList();
            if (fileNames.contains("pom.xml")) {
                return new Maven();
            } else if (fileNames.contains("build.gradle")) {
                return new Gradle();
            }
            throw new IllegalArgumentException("no build tool found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default URL[] findJarUrls(Path dependencyDir) {
        if (!Files.exists(dependencyDir)) {
            return new URL[]{};
        }
        var results = new ArrayList<URL>();
        for (var file : dependencyDir.toFile().listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try {
                    results.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return results.toArray(URL[]::new);
    }
}
