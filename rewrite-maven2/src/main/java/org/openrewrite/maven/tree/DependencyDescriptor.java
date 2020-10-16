package org.openrewrite.maven.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Scope;

import java.util.Set;

public interface DependencyDescriptor {
    String getGroupId();
    String getArtifactId();
    String getVersion();

    @Nullable
    String getClassifier();

    Scope getScope();

    Set<GroupArtifact> getExclusions();
}
