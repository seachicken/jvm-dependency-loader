package inga.jvmdependencyloader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodTest {
    @Test
    void createMethod() throws Exception {
        assertThat(new Method(Class.forName("java.lang.String").getMethod("toString")).name())
                .isEqualTo("java.lang.String.toString");
    }

    @Test
    void getFqNameWithoutDoubleUnderscore() {
        assertThat(Method.getFqName("kotlin.collections.CollectionsKt__CollectionsKt", "listOf"))
                .isEqualTo("kotlin.collections.CollectionsKt.listOf");
    }

    @Test
    void getFqNameWithoutTripleUnderscore() {
        assertThat(Method.getFqName("kotlin.collections.CollectionsKt___CollectionsKt", "distinct"))
                .isEqualTo("kotlin.collections.CollectionsKt.distinct");
    }
}