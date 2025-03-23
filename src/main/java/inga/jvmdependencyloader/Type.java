package inga.jvmdependencyloader;

public record Type(
        String name,
        boolean isInterface,
        boolean isArray
) {
    public Type(Class clazz) {
        this(toLoadableName(clazz), clazz.isInterface(), isArray(clazz));
    }

    private static String toLoadableName(Class clazz) {
        return isArray(clazz)
                ? clazz.componentType().getName()
                : clazz.getName();
    }

    private static boolean isArray(Class clazz) {
        return clazz.componentType() != null;
    }
}
