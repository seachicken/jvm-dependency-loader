package inga.jvmdependencyloader;

import inga.jvmdependencyloader.buildtool.BuildTool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyLoader implements AutoCloseable {
    private final Map<Path, URLClassLoader> classLoaders = new HashMap<>();

    public List<String> getClassPaths(Path from, Path root) {
        System.err.println("begin getClassPaths. from: " + from);
        try (URLClassLoader classLoader = loadClassLoader(from, root)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }
            return Stream.of(classLoaders.get(from).getURLs())
                    .map(URL::getPath)
                    .collect(Collectors.toList());
        } catch (NoClassDefFoundError | Exception e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Method> readMethods(String fqcn, Path from, Path root) {
        try (URLClassLoader classLoader = loadClassLoader(from, root)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var methods = classLoader.loadClass(fqcn).getMethods();
            return Arrays.stream(methods)
                    .map(Method::new)
                    .collect(Collectors.toMap(
                            (m) -> m.name() + m.parameterTypes(),
                            (m) -> m,
                            (a, b) -> a.returnType().isInterface() ? b : a
                    ))
                    .values().stream().toList();
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Clazz> readClasses(String fqcn, Path from, Path root) {
        try (URLClassLoader classLoader = loadClassLoader(from, root)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var classes = classLoader.loadClass(fqcn).getDeclaredClasses();
            return Arrays.stream(classes)
                    .map(c -> new Clazz(c.getName()))
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Type> readHierarchy(String fqcn, Path from, Path root) {
        try (URLClassLoader classLoader = loadClassLoader(from, root)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var results = new ArrayList<Type>();
            var stack = new Stack<Class<?>>();
            stack.push(classLoader.loadClass(fqcn));
            while (!stack.isEmpty()) {
                var clazz = stack.pop();
                results.add(new Type(clazz));
                var parents = new ArrayList<>(List.of(clazz.getInterfaces()));
                if (clazz.getSuperclass() != null) {
                    parents.add(clazz.getSuperclass());
                }
                for (var parent : parents) {
                    if (results.stream().noneMatch(t -> t.name().equals(parent.getName()))
                            && !stack.contains(parent)) {
                        stack.push(parent);
                    }
                }
            }
            Collections.reverse(results);
            return results;
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    private URLClassLoader loadClassLoader(Path from, Path root) {
        if (from == null) {
            return null;
        }
        URLClassLoader classLoader;
        if (classLoaders.containsKey(from)) {
            classLoader = classLoaders.get(from);
        } else {
            classLoader = BuildTool.create(from, root).load();
            classLoaders.put(from, classLoader);
        }

        // recreate the URLClassLoader because compilation path may be added dynamically
        var urls = new HashSet<>(List.of(classLoader.getURLs()));
        try {
            for (var path : BuildTool.create(from, root).findCompiledClassPaths()) {
                urls.add(path.toFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            classLoader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        classLoader = new URLClassLoader(urls.toArray(URL[]::new));
        classLoaders.put(from, classLoader);

        return classLoader;
    }

    @Override
    public void close() throws Exception {
        for (var loader : classLoaders.values()) {
            loader.close();
        }
    }
}
