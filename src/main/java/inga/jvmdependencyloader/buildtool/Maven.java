package inga.jvmdependencyloader.buildtool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class Maven implements BuildTool {
    private final Path root;

    public Maven(Path root) {
        this.root = root;
    }

    @Override
    public URLClassLoader load() {
        copyDependencies();
        return new URLClassLoader(findJarUrls(root.resolve("target/dependency")));
    }

    @Override
    public Path findCompiledClassPath() {
        return root.resolve("target/classes");
    }

    private void copyDependencies() {
        try {
            var process = new ProcessBuilder("mvn", "dependency:copy-dependencies", "-q")
                    .directory(root.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
