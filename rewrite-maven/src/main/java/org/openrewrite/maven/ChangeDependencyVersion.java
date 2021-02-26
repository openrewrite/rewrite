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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyVersion extends Recipe {

    @Option(displayName = "Group ID", description = "Group ID of dependency to modify.")
    String groupId;

    @Option(displayName = "Artifact ID", description = "Artifacty ID of dependency to modify.")
    String artifactId;

    @Option(displayName = "New version", description = "New version to apply to the matching dependency.")
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Change Maven dependency version";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeDependencyVersionVisitor();
    }


    private class ChangeDependencyVersionVisitor extends MavenVisitor {

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(groupId, artifactId) || isManagedDependencyTag(groupId, artifactId)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version != null) {
                        if (version.trim().startsWith("${") && !newVersion.equals(model.getValue(version.trim()))) {
                            doAfterVisit(new ChangePropertyValue(version, newVersion));
                        } else if (!newVersion.equals(version)) {
                            doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), newVersion));
                        }
                    }
                }
                // In this case a transitive dependency has been removed and the dependency now requires a version
                else if (!versionTag.isPresent() && !isManagedDependencyTag(groupId, artifactId)) {
                    Xml.Tag newVersionTag = Xml.Tag.build("<version>"+newVersion+"</version>");
                    doAfterVisit(new AddToTagVisitor<>(getCursor().getValue(), newVersionTag));
                }
            } else if (!modules.isEmpty() && isPropertyTag()) {
                String propertyKeyRef = "${" + tag.getName() + "}";

                OUTER:
                for (Pom module : modules) {
                    for (Pom.Dependency dependency : module.getDependencies()) {
                        if (propertyKeyRef.equals(dependency.getRequestedVersion())) {
                            doAfterVisit(new ChangeTagValueVisitor<>(tag, newVersion));
                            break OUTER;
                        }
                    }

                }
            }

            return super.visitTag(tag, ctx);
        }
    }
}
