package inga.jvmdependencyloader;

import java.util.List;

public record Method(
        String name,
        List<Type> parameterTypes,
        Type returnType
) {}
