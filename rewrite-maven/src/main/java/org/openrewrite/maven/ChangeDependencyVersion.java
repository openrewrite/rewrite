package org.openrewrite.maven;

import org.openrewrite.Validated;

import static org.openrewrite.Validated.required;

public class ChangeDependencyVersion extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String toVersion;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion));
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);

        MavenModel.ModuleVersionId mvid = dependency.getModel().getModuleVersion();
        if (mvid.getGroupId().equals(groupId) && mvid.getArtifactId().equals(artifactId) &&
                !mvid.getVersion().equals(toVersion)) {
            d = d.withVersion(toVersion);
        }

        return d;
    }
}
