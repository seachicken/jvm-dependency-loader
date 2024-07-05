package inga.jvmdependencyloader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyLoaderTest {
    DependencyLoader loader;

    @BeforeEach
    void setUp() {
        loader = new DependencyLoader();
    }

    @AfterEach
    void tierDown() throws Exception {
        loader.close();
    }

    @Nested
    class Gradle {
        @Test
        void readMethodsWithDependencies() {
            var actual = loader.readMethods(
                    "org.joda.time.DateTime",
                    TestHelper.getFixturesPath("spring-boot-realworld-example-app")
            );
            assertThat(actual).size().isEqualTo(144);
        }

        @Test
        void readMethodsWithCompiledClass() {
            var actual = loader.readMethods(
                    "io.spring.core.article.Article",
                    TestHelper.getFixturesPath("spring-boot-realworld-example-app")
            );
            assertThat(actual).size().isEqualTo(19);
        }

        @Test
        void readHierarchy() {
            var actual = loader.readHierarchy(
                    "java.lang.String",
                    TestHelper.getFixturesPath("spring-boot-realworld-example-app")
            ).stream().filter(t -> !t.isInterface()).collect(Collectors.toList());
            assertThat(actual).containsExactly(
                    new Type("java.lang.Object", false, false),
                    new Type("java.lang.String", false, false)
            );
        }
    }

    @Nested
    class Maven {
        @Test
        void readMethodsWithDependencies() {
            var actual = loader.readMethods(
                    "org.springframework.web.util.UriComponentsBuilder",
                    TestHelper.getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).size().isEqualTo(50);
        }

        @Test
        void readMethodsWithCompiledClass() {
            var actual = loader.readMethods(
                    "com.baeldung.apiservice.adapters.tasks.Task",
                    TestHelper.getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).size().isEqualTo(15);
        }

        @Test
        void readMethodsWithList() {
            var actual = loader.readMethods(
                    "java.lang.String",
                    TestHelper.getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).size().isEqualTo(89);
        }

        @Test
        void readClasses() {
            var actual = loader.readClasses(
                    "com.baeldung.classfile.Outer",
                    TestHelper.getFixturesPath("spring-tutorials/core-java-modules/core-java-lang-oop-types")
            );
            assertThat(actual).map(Clazz::name).containsExactlyInAnyOrder(
                    "com.baeldung.classfile.Outer$Color",
                    "com.baeldung.classfile.Outer$HelloOuter",
                    "com.baeldung.classfile.Outer$Nested",
                    "com.baeldung.classfile.Outer$StaticNested"
            );
        }

        @Test
        void readHierarchy() {
            var actual = loader.readHierarchy(
                    "java.lang.String",
                    TestHelper.getFixturesPath("spring-tutorials/lightrun/api-service")
            ).stream().filter(t -> !t.isInterface()).collect(Collectors.toList());
            assertThat(actual).containsExactly(
                    new Type("java.lang.Object", false, false),
                    new Type("java.lang.String", false, false)
            );
        }
    }
}