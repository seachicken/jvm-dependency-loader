package inga.jvmdependencyloader;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Input {
    private Type type;
    private String fqcn;
    private String from;
    private String root;

    public enum Type {
        CLASS_PATHS,
        METHODS,
        CLASSES,
        HIERARCHY
    }
}
