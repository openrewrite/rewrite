package org.openrewrite.gradle.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.io.Serializable;
import java.util.Objects;

@Value
@With
public class ResolvedGroupArtifactVersion implements Serializable {
//    @Nullable
//    String repository;

    String groupId;
    String artifactId;
    String version;

    /**
     * In the form "${version}-${timestamp}-${buildNumber}", e.g. for the artifact rewrite-testing-frameworks-1.7.0-20210614.172805-1.jar,
     * the dated snapshot version is "1.7.0-20210614.172805-1".
     */
//    @Nullable
//    String datedSnapshotVersion;

    @Override
    public String toString() {
        return asGroupArtifactVersion().toString();
    }

    public GroupArtifactVersion asGroupArtifactVersion() {
        return new GroupArtifactVersion(groupId, artifactId, version);
    }

    //    public GroupArtifact asGroupArtifact() {
//        return new GroupArtifact(groupId, artifactId);
//    }

//    public ResolvedGroupArtifactVersion withGroupArtifact(GroupArtifact ga) {
//        if(Objects.equals(ga.getGroupId(), groupId) && Objects.equals(ga.getArtifactId(), artifactId)) {
//            return this;
//        }
//        return new ResolvedGroupArtifactVersion(repository, ga.getGroupId(), ga.getArtifactId(), version, datedSnapshotVersion);
//    }
}
