/*
 * Copyright 2022 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyClassifier extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "guava")
    String artifactId;

    /**
     * If null, strips the scope from an existing dependency.
     */
    @Option(displayName = "New classifier",
            description = "Classifier to apply to specified Maven dependency. " +
                          "May be omitted, which indicates that no classifier should be added and any existing scope be removed from the dependency.",
            example = "jar",
            required = false)
    @Nullable
    String newClassifier;

    @Option(displayName = "Change Maven managed dependency",
            description = "This flag can be set to explicitly change the classifier in Maven management dependency section. Default `false`.",
            example = "true",
            required = false)
    @Nullable
    Boolean changeManagedDependency;

    String displayName = "Change Maven dependency classifier";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s` to `%s`", groupId, artifactId, newClassifier);
    }

    String description = "Add or alter the classifier of the specified dependency.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId) ||
                        (Boolean.TRUE.equals(changeManagedDependency) && isManagedDependencyTag(groupId, artifactId))) {
                    Optional<Xml.Tag> classifier = tag.getChild("classifier");
                    if (isDependencyTag(groupId, artifactId) && !tag.getChild("version").isPresent() &&
                            !Objects.equals(tag.getChildValue("classifier").orElse(null), newClassifier) &&
                            !isTargetManaged(tag)) {
                        ResolvedDependency dependency = findDependency(tag);
                        if (dependency != null) {
                            doAfterVisit(new AddToTagVisitor<>(tag,
                                    Xml.Tag.build("<version>" + dependency.getVersion() + "</version>"),
                                    new MavenTagInsertionComparator(tag.getChildren())));
                        }
                    }
                    if (classifier.isPresent()) {
                        if (newClassifier == null) {
                            doAfterVisit(new RemoveContentVisitor<>(classifier.get(), false, true));
                        } else if (!newClassifier.equals(classifier.get().getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(classifier.get(), newClassifier));
                        }
                    } else if (newClassifier != null) {
                        doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<classifier>" + newClassifier + "</classifier>")));
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean isTargetManaged(Xml.Tag tag) {
                ResolvedPom pom = getResolutionResult().getPom();
                String dependencyGroup = pom.getValue(tag.getChildValue("groupId").orElse(pom.getGroupId()));
                String dependencyArtifact = pom.getValue(tag.getChildValue("artifactId").orElse(""));
                String dependencyType = pom.getValue(tag.getChildValue("type").orElse("jar"));
                if (pom.getManagedDependency(dependencyGroup, dependencyArtifact, dependencyType, pom.getValue(newClassifier)) != null) {
                    return true;
                }
                // A definition in a source POM will receive the same classifier change. Imported
                // BOMs and external parents cannot be edited and do not manage the new coordinate.
                if (Boolean.TRUE.equals(changeManagedDependency)) {
                    ResolvedManagedDependency managed = pom.getManagedDependency(dependencyGroup, dependencyArtifact,
                            dependencyType, pom.getValue(tag.getChildValue("classifier").orElse(null)));
                    MavenResolutionResult current = getResolutionResult();
                    while (managed != null && current != null && current.getPom().getRequested().getSourcePath() != null) {
                        if (current.getPom().getRequested().getDependencyManagement().contains(managed.getRequested())) {
                            return true;
                        }
                        current = current.getParent();
                    }
                }
                return false;
            }
        };
    }
}
