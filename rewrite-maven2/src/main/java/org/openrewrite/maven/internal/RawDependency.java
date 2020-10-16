package org.openrewrite.maven.internal;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Scope;

import java.util.Set;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class RawDependency {
    Scope scope;

    @Nullable
    String classifier;

    boolean optional;
    RawMaven maven;
    Set<GroupArtifact> exclusions;
}
