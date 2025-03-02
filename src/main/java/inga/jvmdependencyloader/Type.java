package inga.jvmdependencyloader;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Type {
    private final String name;
    private final boolean isInterface;
    private final boolean isArray;

    public Type(Class<?> clazz) {
        name = toLoadableName(clazz);
        isInterface = clazz.isInterface();
        isArray = isArray(clazz);
    }

    private static String toLoadableName(Class<?> clazz) {
        return isArray(clazz)
                ? clazz.getComponentType().getName()
                : clazz.getName();
    }

    private static boolean isArray(Class<?> clazz) {
        return clazz.getComponentType() != null;
    }
}
