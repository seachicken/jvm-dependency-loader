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

    public List<Method> readMethods(String fqcn, Path from) {
        URLClassLoader classLoader = loadClassLoader(from);
        if (classLoader == null) {
            return Collections.emptyList();
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

    public List<Type> readHierarchy(String fqcn, Path from) {
        URLClassLoader classLoader = loadClassLoader(from);
        if (classLoader == null) {
            return Collections.emptyList();
        }

        try {
            var clazz = classLoader.loadClass(fqcn);
            var results = new ArrayList<Type>();
            while (clazz != null) {
                results.add(new Type(clazz.getName(), clazz.isInterface()));
                clazz = clazz.getSuperclass();
            }
            Collections.reverse(results);
            return results;
        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private URLClassLoader loadClassLoader(Path from) {
        var baseDir = classLoaders.keySet()
                .stream()
                .filter(p -> p.toString().startsWith(p.toString()))
                .findFirst()
                .orElse(null);
        if (baseDir == null) {
            baseDir = from;
        }
        if (baseDir == null) {
            return null;
        }
        URLClassLoader classLoader;
        if (classLoaders.containsKey(baseDir)) {
            classLoader = classLoaders.get(baseDir);
        } else {
            copyDependencies(baseDir);
            classLoader = new URLClassLoader(findJarUrls(baseDir.resolve("target/dependency")));
            classLoaders.put(baseDir, classLoader);
        }
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        for (var loader : classLoaders.values()) {
            loader.close();
        }
    }

    private void copyDependencies(Path path) {
        try {
            var process = new ProcessBuilder("mvn", "dependency:copy-dependencies", "-q")
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
