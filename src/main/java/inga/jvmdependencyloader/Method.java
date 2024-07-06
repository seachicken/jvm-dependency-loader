package inga.jvmdependencyloader;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record Method(
        String name,
        List<Type> parameterTypes,
        Type returnType
) {
    public Method(java.lang.reflect.Method method) {
        this(getFqName(method.getDeclaringClass().getName(), method.getName()),
                Arrays.stream(method.getParameterTypes())
                        .map(Type::new)
                        .collect(Collectors.toList()),
                new Type(method.getReturnType())
        );
    }

    static String getFqName(String className, String methodName) {
        int index = className.lastIndexOf("___");
        if (index < 0) {
            index = className.lastIndexOf("__");
        }
        return (index > 0 ? className.substring(0, index) : className) + "." + methodName;
    }
}
