package inga.jvmdependencyresolver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DependencyResolver implements AutoCloseable {
    private URLClassLoader classLoader;

    public void loadProject(String inputPath) {
        var path = Path.of(inputPath);
        copyDependencies(path);

        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        classLoader = new URLClassLoader(findJarUrls(path.resolve("target/dependency")));
    }

    public List<Method> readMethods(String fqcn) {
        if (classLoader == null) {
            throw new IllegalStateException("project not loaded");
        }
        try {
            var methods = classLoader.loadClass(fqcn).getDeclaredMethods();
            return Arrays.stream(methods)
                    .map(m -> new Method(
                            m.getName(),
                            Arrays.stream(m.getParameterTypes()).map(Class::getName).collect(Collectors.toList()),
                            m.getReturnType().getName()
                    ))
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (classLoader != null) {
            classLoader.close();
        }
    }

    private void copyDependencies(Path path) {
        if (!Files.exists(path.resolve("pom.xml"))) {
            throw new IllegalArgumentException("pom.xml not found");
        }

        try {
            var process = new ProcessBuilder("mvn", "dependency:copy-dependencies")
                .directory(path.toFile())
                .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL[] findJarUrls(Path dependencyDir) {
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
