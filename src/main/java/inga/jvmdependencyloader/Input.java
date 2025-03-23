package inga.jvmdependencyloader;

import lombok.Data;

@Data
public class Input {
    private final Type type;
    private final String fqcn;
    private final String from;
    private final String root;

    public enum Type {
        CLASS_PATHS,
        METHODS,
        CLASSES,
        HIERARCHY
    }
}
