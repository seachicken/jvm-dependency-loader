package inga.jvmdependencyresolver;

import java.util.List;

public record Method(
        String name,
        List<String> parameterTypes,
        String returnType
) {}
