/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveExclusion extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Exclusion group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String exclusionGroupId;

    @Option(displayName = "Exclusion artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String exclusionArtifactId;

    @Option(displayName = "Only ineffective",
            description = "Default false. If enabled, matching exclusions will only be removed if they are ineffective (if the excluded dependency was not actually a transitive dependency of the target dependency).",
            required = false)
    @Nullable
    Boolean onlyIneffective;

    @Override
    public String getDisplayName() {
        return "Remove exclusion";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", exclusionGroupId, exclusionArtifactId);
    }

    @Override
    public String getDescription() {
        return "Remove any matching exclusion from any matching dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId) || isManagedDependencyTag(groupId, artifactId)) {
                    Optional<Xml.Tag> maybeExclusions = tag.getChild("exclusions");
                    if (maybeExclusions.isPresent()) {
                        //noinspection unchecked
                        return tag.withContent(ListUtils.map((List<Content>) tag.getContent(), child -> {
                            if (child instanceof Xml.Tag && "exclusions".equals(((Xml.Tag) child).getName())) {
                                Xml.Tag e = (Xml.Tag) child;
                                if (e.getContent() != null) {
                                    e = e.withContent(ListUtils.map(e.getContent(), child2 -> {
                                        if (child2 instanceof Xml.Tag && "exclusion".equals(((Xml.Tag) child2).getName())) {
                                            Xml.Tag exclusion = (Xml.Tag) child2;
                                            if (exclusion.getChildValue("groupId").map(g -> matchesGlob(g, exclusionGroupId)).orElse(false) &&
                                                exclusion.getChildValue("artifactId").map(g -> matchesGlob(g, exclusionArtifactId)).orElse(false) &&
                                                !(isEffectiveExclusion(tag, groupArtifact(exclusion)) && Boolean.TRUE.equals(onlyIneffective))) {
                                                return null;
                                            }
                                        }
                                        return child2;
                                    }));

                                    if (e.getContent() == null || e.getContent().isEmpty()) {
                                        return null;
                                    } else if (e.getContent().stream().noneMatch(Xml.Tag.class::isInstance)) {
                                        return null;
                                    }
                                }
                                return e;
                            }
                            return child;
                        }));
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private GroupArtifact groupArtifact(Xml.Tag tag) {
                return new GroupArtifact(
                        tag.getChildValue("groupId").orElseThrow(IllegalArgumentException::new),
                        tag.getChildValue("artifactId").orElseThrow(IllegalArgumentException::new)
                );
            }

            private boolean isEffectiveExclusion(Xml.Tag tag, GroupArtifact exclusion) {
                final ResolvedDependency dependency = findDependency(tag);
                if (dependency != null) {
                    return dependency.getEffectiveExclusions().contains(exclusion);
                }
                final ResolvedManagedDependency managedDependency = findManagedDependency(tag);
                // With current code, if a dependency is only in dependencyManagement (and not an active dependency in some scope),
                // then we never resolve its transitive dependencies and therefore cannot check if exclusions are effective.
                // So, default to assuming those exclusions are effective, to avoid incorrect removals.
                // Meanwhile, exclusions on bom imports aren't actually implemented in Maven (src https://issues.apache.org/jira/browse/MNG-5600),
                // so those are always ineffective
                return managedDependency != null && managedDependency.getRequested() != null;
            }
        };
    }
}
