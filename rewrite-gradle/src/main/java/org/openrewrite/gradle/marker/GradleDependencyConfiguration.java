package org.openrewrite.gradle.marker;

import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;

@Value
@With
public class GradleDependencyConfiguration {

    /**
     * The name of the dependency configuration. Unique within a given project.
     */
    String name;

    @Nullable
    String description;

    boolean isTransitive;

    boolean isCanBeResolved;

    /**
     * The list of zero or more configurations this configuration extends from.
     * The extended configuration's dependencies are all requested as part of this configuration, but different versions
     * may be resolved.
     */
    @NonFinal
    List<GradleDependencyConfiguration> extendsFrom;

    List<org.openrewrite.maven.tree.Dependency> requested;
    List<org.openrewrite.maven.tree.ResolvedDependency> resolved;

    void unsafeSetExtendsFrom(List<GradleDependencyConfiguration> extendsFrom) {
        this.extendsFrom = extendsFrom;
    }
}
