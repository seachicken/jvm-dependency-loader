package inga.jvmdependencyloader;

import inga.jvmdependencyloader.buildtool.BuildTool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyLoader implements AutoCloseable {
    private final Map<Path, URLClassLoader> classLoaders = new HashMap<>();

    public List<String> getClassPaths(Path from, Path root) {
        try (URLClassLoader classLoader = loadClassLoader(from, root)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }
            return Stream.of(classLoaders.get(from).getURLs())
                    .map(URL::getPath)
                    .collect(Collectors.toList());
        } catch (NoClassDefFoundError | IOException e) {
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

            java.lang.reflect.Method[] methods = classLoader.loadClass(fqcn).getMethods();
            return new ArrayList<>(Arrays.stream(methods)
                    .map(Method::new)
                    .collect(Collectors.toMap(
                            (m) -> m.getName() + m.getParameterTypes(),
                            (m) -> m,
                            (a, b) -> a.getReturnType().isInterface() ? b : a
                    ))
                    .values());
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

            Class<?>[] classes = classLoader.loadClass(fqcn).getDeclaredClasses();
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

            ArrayList<Type> results = new ArrayList<>();
            Stack<Class<?>> stack = new Stack<>();
            stack.push(classLoader.loadClass(fqcn));
            while (!stack.isEmpty()) {
                Class<?> clazz = stack.pop();
                results.add(new Type(clazz));
                ArrayList<Class<?>> parents = new ArrayList<>(Arrays.asList(clazz.getInterfaces()));
                if (clazz.getSuperclass() != null) {
                    parents.add(clazz.getSuperclass());
                }
                for (Class<?> parent : parents) {
                    if (results.stream().noneMatch(t -> t.getName().equals(parent.getName()))
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
        HashSet<URL> urls = new HashSet<>(Arrays.asList(classLoader.getURLs()));
        try {
            for (Path path : BuildTool.create(from, root).findCompiledClassPaths()) {
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
        classLoader = new URLClassLoader(new ArrayList<>(urls).toArray(new URL[0]));
        classLoaders.put(from, classLoader);

        return classLoader;
    }

    @Override
    public void close() throws Exception {
        for (URLClassLoader loader : classLoaders.values()) {
            loader.close();
        }
    }
}
