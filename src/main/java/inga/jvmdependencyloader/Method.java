package inga.jvmdependencyloader;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Method {
    private final String name;
    private final List<Type> parameterTypes;
    private final Type returnType;

    public Method(java.lang.reflect.Method method) {
        name = getFqName(method.getDeclaringClass().getName(), method.getName());
        parameterTypes = Arrays
                .stream(method.getParameterTypes())
                .map(Type::new)
                .collect(Collectors.toList());
        returnType = new Type(method.getReturnType());
    }

    static String getFqName(String className, String methodName) {
        int index = className.lastIndexOf("___");
        if (index < 0) {
            index = className.lastIndexOf("__");
        }
        return (index > 0 ? className.substring(0, index) : className) + "." + methodName;
    }
}
