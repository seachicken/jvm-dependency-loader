package inga.jvmdependencyloader.buildtool;

import inga.jvmdependencyloader.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

class GradleTest {
    private Gradle gradle;

    @BeforeEach
    void setUp() {
        gradle = new Gradle(
                TestHelper.getFixturesPath("spring-boot-realworld-example-app"),
                TestHelper.getFixturesPath("spring-boot-realworld-example-app")
        );
    }

    @Test
    void loadWithDirectDependency() throws Exception {
        URLClassLoader classLoader = gradle.load();
        assertThat(classLoader.getURLs())
                .extracting(URL::getPath)
                .anyMatch(p -> p.endsWith("mybatis-spring-boot-starter-2.2.2.jar"));
        classLoader.close();
    }

    @Test
    void loadWithTransitiveDependency() throws Exception {
        URLClassLoader classLoader = gradle.load();
        assertThat(classLoader.getURLs())
                .extracting(URL::getPath)
                .anyMatch(p -> p.endsWith("spring-boot-starter-validation-2.6.3.jar"));
        classLoader.close();
    }
}