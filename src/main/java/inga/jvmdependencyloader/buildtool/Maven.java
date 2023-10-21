package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class Maven implements BuildTool {
    @Override
    public URLClassLoader load(Path root) {
        copyDependencies(root);
        return new URLClassLoader(findJarUrls(root.resolve("target/dependency")));
    }

    private void copyDependencies(Path path) {
        try {
            var process = new ProcessBuilder("mvn", "dependency:copy-dependencies", "-q")
                    .directory(path.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
