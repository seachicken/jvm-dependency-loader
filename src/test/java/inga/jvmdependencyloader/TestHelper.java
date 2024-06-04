package inga.jvmdependencyloader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHelper {
    public static Path getFixturesPath(String path) {
        return Paths.get("src","test","resources", "fixtures", path).toAbsolutePath();
    }
}
