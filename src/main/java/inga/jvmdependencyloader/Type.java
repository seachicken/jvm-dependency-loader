package inga.jvmdependencyloader;

public record Type(
        String name,
        boolean isInterface
) {
    public Type(Class clazz, boolean isInterface) {
        this(clazz.getCanonicalName(), isInterface);
    }
}
