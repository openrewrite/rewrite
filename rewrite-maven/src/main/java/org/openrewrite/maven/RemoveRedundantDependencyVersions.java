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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRedundantDependencyVersions extends Recipe {
    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                    "Group is the the first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.*",
            required = false)
    @Nullable
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                    "Artifact is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Only if versions match",
            description = "Only remove the explicit version if it matches the managed dependency version. Default true.",
            example = "false",
            required = false)
    @Nullable
    Boolean onlyIfVersionsMatch;

    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependency versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions when a parent POM's dependencyManagement " +
                "specifies the version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!isManagedDependencyTag()) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null && matchesVersion(d) && matchesScope(d, tag) &&
                            matchesGroup(d) && matchesArtifact(d)) {
                        Xml.Tag version = tag.getChild("version").orElse(null);
                        return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean matchesGroup(ResolvedDependency d) {
                return groupPattern == null || matchesGlob(d.getGroupId(), groupPattern);
            }

            private boolean matchesArtifact(ResolvedDependency d) {
                return artifactPattern == null || matchesGlob(d.getArtifactId(), artifactPattern);
            }

            private boolean matchesVersion(ResolvedDependency d) {
                return ignoreVersionMatching() ||
                        d.getRequested().getVersion() != null
                                && d.getRequested().getVersion().equals(getResolutionResult().getPom().getManagedVersion(d.getGroupId(), d.getArtifactId(),
                                d.getRequested().getType(), d.getRequested().getClassifier()));
            }

            private boolean ignoreVersionMatching() {
                return Boolean.FALSE.equals(onlyIfVersionsMatch);
            }

            private boolean matchesScope(ResolvedDependency d, Xml.Tag dependencyTag) {
                return Objects.equals(
                        Scope.fromName(dependencyTag.getChildValue("scope").orElse(null)),
                        getResolutionResult().getPom().getManagedScope(d.getGroupId(), d.getArtifactId(), d.getRequested().getType(),
                                d.getRequested().getClassifier()));
            }
        };
    }
}
