package inga.jvmdependencyloader;

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
    private final Map<Path, Path> baseDirCache = new HashMap<>();

    public List<Method> readMethods(String fqcn, Path from) {
        var baseDir = findProjectBaseDir(from);
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
        if (baseDirCache.containsKey(path)) {
            return baseDirCache.get(path);
        } else {
            Path currentPath = path.getParent();
            while ((currentPath = currentPath.getParent()) != null) {
                var baseDir = evaluateBaseDir(currentPath);
                if (baseDir.equals("null object or invalid expression")) {
                    continue;
                }
                baseDirCache.put(path, Path.of(baseDir));
                return Path.of(baseDir);
            }
        }
        return null;
    }

    private String evaluateBaseDir(Path path) {
        try {
            var process = new ProcessBuilder("mvn", "help:evaluate", "-Dexpression=project.basedir", "-q", "-DforceStdout")
                    .directory(path.toFile())
                    .start();
            process.waitFor();
            return new String(process.getInputStream().readAllBytes());
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
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
