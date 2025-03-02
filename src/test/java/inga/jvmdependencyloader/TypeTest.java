package inga.jvmdependencyloader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeTest {
    @Test
    void convertNameByClassLoaderWithArray() {
        // L[java.lang.String; -> java.lang.String
        assertThat(new Type(String[].class).getName()).isEqualTo("java.lang.String");
    }
}