package inga.jvmdependencyloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyLoader implements AutoCloseable {
    private final Map<Path, URLClassLoader> classLoaders = new HashMap<>();

    public List<Method> readMethods(String fqcn, Path from) {
        var baseDir = classLoaders.keySet()
                .stream()
                .filter(p -> p.toString().startsWith(p.toString()))
                .findFirst()
                .orElse(null);
        if (baseDir == null) {
            baseDir = findProjectBaseDir(from);
        }
        if (baseDir == null) {
            return Collections.emptyList();
        }
        URLClassLoader classLoader;
        if (classLoaders.containsKey(baseDir)) {
            classLoader = classLoaders.get(baseDir);
        } else {
            copyDependencies(baseDir);
            classLoader = new URLClassLoader(findJarUrls(baseDir.resolve("target/dependency")));
            classLoaders.put(baseDir, classLoader);
        }

        try {
            var methods = classLoader.loadClass(fqcn).getDeclaredMethods();
            return Arrays.stream(methods)
                    .map(m -> new Method(
                            m.getName(),
                            Arrays.stream(m.getParameterTypes())
                                    .map(t -> new Type(t.getName(), t.isInterface()))
                                    .collect(Collectors.toList()),
                            new Type(m.getReturnType().getName(), m.getReturnType().isInterface())
                    ))
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void close() throws Exception {
        for (var loader : classLoaders.values()) {
            loader.close();
        }
    }

    private Path findProjectBaseDir(Path path) {
        Path currentPath = path.getParent();
        while ((currentPath = currentPath.getParent()) != null) {
            if (Arrays.stream(currentPath.toFile().listFiles())
                    .filter(File::isFile)
                    .anyMatch(f -> List.of("pom.xml", "build.gradle").contains(f.getName()))) {
                return currentPath;
            }
        }
        return null;
    }

    private void copyDependencies(Path path) {
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
