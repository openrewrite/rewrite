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

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.RemoveContent;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class RemoveDependency extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;

    public RemoveDependency() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (isDependencyTag(groupId, artifactId)) {
            andThen(new RemoveContent.Scoped(tag, true));
        }

        return super.visitTag(tag);
    }

    @Override
    public Maven visitMaven(Maven maven) {
        model = maven.getModel();
        if(findDependencies(groupId, artifactId).size() == 0) {
            return maven;
        }
        Maven m = super.visitMaven(maven);
        List<Pom.Dependency> dependencies = model.getDependencies().stream()
                .filter(dep -> !(dep.getArtifactId().equals(artifactId) && dep.getGroupId().equals(groupId)))
                .collect(toList());

        return m.withModel(model.withDependencies(dependencies));
    }
}
