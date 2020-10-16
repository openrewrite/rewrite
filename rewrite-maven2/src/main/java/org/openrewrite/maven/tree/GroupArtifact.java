
package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class GroupArtifact {
    String groupId;
    String artifactId;
}
