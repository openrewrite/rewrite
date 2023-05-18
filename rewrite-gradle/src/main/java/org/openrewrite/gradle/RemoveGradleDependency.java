package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@EqualsAndHashCode(callSuper = true)
@Deprecated // Replaced by RemoveDependency
public class RemoveGradleDependency extends Recipe {
    @Option(displayName = "The dependency configuration", description = "The dependency configuration to remove from.", example = "api", required = false)
    @Nullable
    private final String configuration;

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    private final String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    private final String artifactId;

    @Override
    public String getDisplayName() {
        return "Remove a Gradle dependency";
    }

    @Override
    public String getDescription() {
        return "Deprecated form of `RemoveDependency`. Use that instead.";
    }

    public RemoveGradleDependency(String configuration, String groupId, String artifactId) {
        this.configuration = configuration;
        this.groupId = groupId;
        this.artifactId = artifactId;
        doNext(new RemoveDependency(groupId, artifactId, configuration));
    }
}
