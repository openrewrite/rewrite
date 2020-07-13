/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;

import java.util.Optional;

import static org.openrewrite.Validated.required;

public class ChangeDependencyVersion extends MavenRefactorVisitor {
    private String groupId;

    @Nullable
    private String artifactId;

    private String toVersion;

    public ChangeDependencyVersion() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(@Nullable String artifactId) {
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
        Maven.Property p = refactor(property, super::visitProperty);
        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);

        if (!property.getValue().equals(toVersion) &&
                property.isDependencyVersionProperty(pom, groupId, artifactId)) {
            p = p.withValue(toVersion);
        }

        return p;
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);

        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);
        assert pom != null;

        MavenModel.ModuleVersionId mvid = dependency.getModel().getModuleVersion();
        if (mvid.getGroupId().equals(groupId) && (artifactId == null || mvid.getArtifactId().equals(artifactId))) {
            if (!mvid.getVersion().equals(toVersion)) {
                Optional<Maven.Property> property = pom.getPropertyFromValue(
                        dependency.getVersion());
                if (property.isPresent()) {
                    andThen(new ChangePropertyValue.Scoped(property.get(), toVersion));
                } else {
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
