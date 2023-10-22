package inga.jvmdependencyloader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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
                    getFixturesPath("spring-boot-realworld-example-app")
            );
            assertThat(actual).size().isEqualTo(89);
        }

        @Test
        void readMethodsWithCompiledClass() throws Exception {
            compile(getFixturesPath("spring-boot-realworld-example-app"));
            var actual = loader.readMethods(
                    "io.spring.core.article.Article",
                    getFixturesPath("spring-boot-realworld-example-app")
            );
            assertThat(actual).size().isEqualTo(14);
        }

        @Test
        void readHierarchy() {
            var actual = loader.readHierarchy(
                    "java.lang.String",
                    getFixturesPath("spring-boot-realworld-example-app")
            );
            assertThat(actual).containsExactly(
                    new Type("java.lang.Object", false),
                    new Type("java.lang.String", false)
            );
        }

        private void compile(Path root) throws Exception {
            var process = new ProcessBuilder("./gradlew", "clean", "compileJava")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        }
    }

    @Nested
    class Maven {
        @Test
        void readMethodsWithDependencies() {
            var actual = loader.readMethods(
                    "org.springframework.web.util.UriComponentsBuilder",
                    getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).size().isEqualTo(68);
        }

        @Test
        void readMethodsWithCompiledClass() throws Exception {
            compile(getFixturesPath("spring-tutorials/lightrun"));
            var actual = loader.readMethods(
                    "com.baeldung.apiservice.adapters.tasks.Task",
                    getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).size().isEqualTo(9);
        }

        @Test
        void readHierarchy() {
            var actual = loader.readHierarchy(
                    "java.lang.String",
                    getFixturesPath("spring-tutorials/lightrun/api-service")
            );
            assertThat(actual).containsExactly(
                    new Type("java.lang.Object", false),
                    new Type("java.lang.String", false)
            );
        }

        private void compile(Path root) throws Exception {
            var process = new ProcessBuilder("mvn", "clean", "compile")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        }
    }

    private Path getFixturesPath(String path) {
        return Path.of(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}