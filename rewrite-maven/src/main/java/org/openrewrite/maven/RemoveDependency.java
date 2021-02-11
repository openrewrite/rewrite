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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@EqualsAndHashCode(callSuper = true)
public class RemoveDependency extends Recipe {
    private final String groupId;
    private final String artifactId;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDependencyVisitor();
    }

    private class RemoveDependencyVisitor extends MavenVisitor {

        public RemoveDependencyVisitor() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(groupId, artifactId)) {
                doAfterVisit(new RemoveContentVisitor<>(tag, true));
            }

            return super.visitTag(tag, ctx);
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            model = maven.getModel();
            if (findDependencies(groupId, artifactId).size() == 0) {
                return maven;
            }
            Maven m = super.visitMaven(maven, ctx);
            List<Pom.Dependency> dependencies = model.getDependencies().stream()
                    .filter(dep -> !(dep.getArtifactId().equals(artifactId) && dep.getGroupId().equals(groupId)))
                    .collect(toList());

            return m.withModel(model.withDependencies(dependencies));
        }
    }
}
