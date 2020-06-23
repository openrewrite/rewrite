package org.openrewrite.maven;

import org.openrewrite.Validated;

import java.util.Optional;

import static org.openrewrite.Validated.required;

public class ChangeDependencyVersion extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String toVersion;

    public ChangeDependencyVersion() {
        setCursoringOn();
    }

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
    public Maven visitProperty(Maven.Property property) {
        return super.visitProperty(property);
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);

        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);
        assert pom != null;

        MavenModel.ModuleVersionId mvid = dependency.getModel().getModuleVersion();
        if (mvid.getGroupId().equals(groupId) && mvid.getArtifactId().equals(artifactId)) {
            if (!mvid.getVersion().equals(toVersion)) {
                Optional<Maven.Property> property = pom.getPropertyFromValue(
                        dependency.getVersion());
                if(property.isPresent()) {
                    andThen(new ChangePropertyValue.Scoped(property.get(), toVersion));
                }
                else {
                    andThen(new Scoped(d, toVersion));
                }
            }
        }

        return d;
    }

    public static class Scoped extends MavenRefactorVisitor {
        private final Maven.Dependency scope;
        private final String toVersion;

        public Scoped(Maven.Dependency scope, String toVersion) {
            this.scope = scope;
            this.toVersion = toVersion;
        }

        @Override
        public Maven visitDependency(Maven.Dependency dependency) {
            Maven.Dependency d = refactor(dependency, super::visitDependency);
            if (scope.isScope(dependency)) {
                d = d.withVersion(toVersion);
            }
            return d;
        }
    }
}
