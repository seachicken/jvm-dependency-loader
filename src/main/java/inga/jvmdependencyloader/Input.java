package inga.jvmdependencyloader;

public record Input(
        Type type,
        String fqcn,
        String from
) {
    public enum Type {
        METHODS,
        HIERARCHY
    }
}
