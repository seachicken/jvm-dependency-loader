package inga.jvmdependencyloader;

import inga.jvmdependencyloader.buildtool.BuildTool;

import java.net.URLClassLoader;
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
            var methods = classLoader.loadClass(fqcn).getMethods();
            return Arrays.stream(methods)
                    .map(m -> new Method(
                            m.getName(),
                            Arrays.stream(m.getParameterTypes())
                                    .map(t -> new Type(t, t.isInterface()))
                                    .collect(Collectors.toList()),
                            new Type(m.getReturnType(), m.getReturnType().isInterface())
                    ))
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Type> readHierarchy(String fqcn, Path from) {
        URLClassLoader classLoader = loadClassLoader(from);
        if (classLoader == null) {
            return Collections.emptyList();
        }

        try {
            var results = new ArrayList<Type>();
            var stack = new Stack<Class<?>>();
            stack.push(classLoader.loadClass(fqcn));
            while (!stack.isEmpty()) {
                var clazz = stack.pop();
                results.add(new Type(clazz.getName(), clazz.isInterface()));
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
        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private URLClassLoader loadClassLoader(Path from) {
        System.err.println("loadClassLoader. from: " + from + " classLoaders: " + classLoaders.size());
        if (from == null) {
            return null;
        }
        URLClassLoader classLoader;
        if (classLoaders.containsKey(from)) {
            System.err.println(" found key");
            classLoader = classLoaders.get(from);
        } else {
            System.err.println(" not found key");
            classLoader = BuildTool.create(from).load();
            classLoaders.put(from, classLoader);
        }
        System.err.println(" get loader: " + classLoader);
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        for (var loader : classLoaders.values()) {
            loader.close();
        }
    }
}
