package inga.jvmdependencyloader;

public record Input(
        Type type,
        String fqcn,
        String from,
        String root
) {
    public enum Type {
        METHODS,
        CLASSES,
        HIERARCHY
    }
}
