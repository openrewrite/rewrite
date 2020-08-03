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
import org.openrewrite.xml.tree.Content;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.required;

public class ChangeDependencyScope extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;

    public ChangeDependencyScope() {
        setCursoringOn();
    }

    /**
     * If null, strips the scope from an existing dependency.
     */
    @Nullable
    private String toScope;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToScope(@Nullable String toScope) {
        this.toScope = toScope;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId));
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);

        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);
        assert pom != null;

        MavenModel.ModuleVersionId mvid = dependency.getModel().getModuleVersion();
        if (mvid.getGroupId().equals(groupId) && (artifactId == null || mvid.getArtifactId().equals(artifactId))) {
            String scope = dependency.getScope();
            if (scope == null && toScope != null) {
                andThen(new Scoped(d, toScope));
            } else if (toScope == null && scope != null) {
                andThen(new Scoped(d, null));
            } else if (scope != null && !scope.equals(toScope)) {
                andThen(new Scoped(d, toScope));
            }
        }

        return d;
    }

    public static class Scoped extends MavenRefactorVisitor {
        private final Maven.Dependency scope;
        private final String toScope;

        public Scoped(Maven.Dependency scope, String toScope) {
            this.scope = scope;
            this.toScope = toScope;
        }

        @Override
        public Maven visitDependency(Maven.Dependency dependency) {
            Maven.Dependency d = refactor(dependency, super::visitDependency);
            if (scope.isScope(dependency)) {
                if (toScope == null) {
                    d = d.withTag(d.getTag().withContent(d.getTag().getChildren().stream()
                            .filter(t -> !t.getName().equals("scope"))
                            .map(Content.class::cast)
                            .collect(toList())));
                } else {
                    d = d.withScope(toScope);
                }
            }
            return d;
        }
    }
}
