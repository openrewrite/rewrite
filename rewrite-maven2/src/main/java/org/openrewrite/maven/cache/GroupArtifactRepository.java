package org.openrewrite.maven.cache;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.maven.tree.GroupArtifact;

import java.net.URL;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
class GroupArtifactRepository {
    URL repository;
    GroupArtifact groupArtifact;
}
