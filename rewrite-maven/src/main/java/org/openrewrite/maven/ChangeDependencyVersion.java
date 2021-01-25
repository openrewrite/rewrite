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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.Validated.required;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyVersion extends Recipe {
    private final String groupId;

    @Nullable
    private final String artifactId;

    private final String toVersion;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeDependencyVersionVisitor();
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion));
    }

    private class ChangeDependencyVersionVisitor extends MavenVisitor<ExecutionContext> {

        public ChangeDependencyVersionVisitor() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(groupId, artifactId) || isManagedDependencyTag(groupId, artifactId)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version != null) {
                        if (version.trim().startsWith("${") &&
                                !toVersion.equals(model.getProperty(version.trim()))) {
                            ChangePropertyValue changePropertyValue = new ChangePropertyValue(version, toVersion);
                            doAfterVisit(changePropertyValue);
                        } else if (!toVersion.equals(version)) {
                            doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), toVersion));
                        }
                    }
                }
            } else if (!modules.isEmpty() && isPropertyTag()) {
                String propertyKeyRef = "${" + tag.getName() + "}";

                OUTER:
                for (Pom module : modules) {
                    for (Pom.Dependency dependency : module.getDependencies()) {
                        if (propertyKeyRef.equals(dependency.getRequestedVersion())) {
                            doAfterVisit(new ChangeTagValueVisitor<>(tag, toVersion));
                            break OUTER;
                        }
                    }

                }
            }

            return super.visitTag(tag, ctx);
        }
    }
}
