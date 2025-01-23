package inga.jvmdependencyloader;

public record Input(
        Type type,
        String fqcn,
        String from,
        String root
) {
    public enum Type {
        CLASS_PATHS,
        METHODS,
        CLASSES,
        HIERARCHY
    }
}
