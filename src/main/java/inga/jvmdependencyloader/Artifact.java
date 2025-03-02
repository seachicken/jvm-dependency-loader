package inga.jvmdependencyloader;

import lombok.Data;

@Data
public class Artifact {
    private final String groupId;
    private final String artifactId;
    private final String version;
}
