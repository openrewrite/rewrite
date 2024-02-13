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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove duplicate Maven dependencies";
    }

    @Override
    public String getDescription() {
        return "Removes duplicated dependencies in the `<dependencies>` and `<dependencyManagement>` sections of the `pom.xml`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (root.getChild("dependencies").isPresent() || root.getChild("dependencyManagement").isPresent()) {
                    return SearchResult.found(document);
                }
                return document;
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            private final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");
            private final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

            @SuppressWarnings("DataFlowIssue")
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependenciesTag()) {
                    getCursor().putMessage("dependencies", new HashMap<DependencyKey, Xml.Tag>());
                } else if (isManagedDependenciesTag()) {
                    getCursor().putMessage("managedDependencies", new HashMap<DependencyKey, Xml.Tag>());
                } else if (isDependencyTag()) {
                    Map<DependencyKey, Xml.Tag> dependencies = getCursor().getNearestMessage("dependencies");
                    DependencyKey dependencyKey = getDependencyKey(tag);
                    if (dependencyKey != null) {
                        Xml.Tag existing = dependencies.putIfAbsent(dependencyKey, tag);
                        if (existing != null && existing != tag) {
                            maybeUpdateModel();
                            return null;
                        }
                    }
                } else if (isManagedDependencyTag()) {
                    Map<DependencyKey, Xml.Tag> dependencies = getCursor().getNearestMessage("managedDependencies");
                    DependencyKey dependencyKey = getManagedDependencyKey(tag);
                    if (dependencyKey != null) {
                        Xml.Tag existing = dependencies.putIfAbsent(dependencyKey, tag);
                        if (existing != null && existing != tag) {
                            maybeUpdateModel();
                            return null;
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean isDependenciesTag() {
                return DEPENDENCIES_MATCHER.matches(getCursor());
            }

            private boolean isManagedDependenciesTag() {
                return MANAGED_DEPENDENCIES_MATCHER.matches(getCursor());
            }

            @Nullable
            private DependencyKey getDependencyKey(Xml.Tag tag) {
                Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
                Scope scope = tag.getChildValue("scope").map(Scope::fromName).orElse(Scope.Compile);
                if (dependencies.containsKey(scope)) {
                    for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                        Dependency req = resolvedDependency.getRequested();
                        String reqGroup = req.getGroupId();
                        if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                            Objects.equals(req.getArtifactId(), tag.getChildValue("artifactId").orElse(null)) &&
                            Objects.equals(Optional.ofNullable(req.getType()).orElse("jar"), tag.getChildValue("type").orElse("jar")) &&
                            Objects.equals(req.getClassifier(), tag.getChildValue("classifier").orElse(null))) {
                            return DependencyKey.from(resolvedDependency, scope);
                        }
                    }
                }
                return null;
            }

            @Nullable
            private DependencyKey getManagedDependencyKey(Xml.Tag tag) {
                ResolvedManagedDependency resolvedDependency = findManagedDependency(tag);
                return resolvedDependency != null ? DependencyKey.from(resolvedDependency) : null;
            }
        });
    }

    @Value
    private static class DependencyKey {
        @Nullable
        String groupId;

        String artifactId;
        String type;

        @Nullable
        String classifier;

        Scope scope;

        public static DependencyKey from(ResolvedDependency dependency, Scope scope) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), scope);
        }

        public static DependencyKey from(ResolvedManagedDependency dependency) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), Scope.Compile);
        }
    }
}
