package inga.jvmdependencyloader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void readMethods() {
        var actual = loader.readMethods(
                "org.springframework.web.util.UriComponentsBuilder",
                getFixturesPath("spring-tutorials/lightrun/api-service")
        );
        assertThat(actual).size().isEqualTo(68);
    }

    private Path getFixturesPath(String path) {
        return Path.of(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}