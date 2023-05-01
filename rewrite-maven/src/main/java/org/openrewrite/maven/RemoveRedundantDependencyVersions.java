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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRedundantDependencyVersions extends Recipe {
    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be managed." +
                    "Group is the first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.*",
            required = false)
    @Nullable
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be managed." +
                    "Artifact is the second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Only if versions match",
            description = "Only remove the explicit version if it matches the managed dependency version. Default true.",
            required = false)
    @Nullable
    Boolean onlyIfVersionsMatch;

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs. Dependencies matching a GAV will be ignored by this recipe."
                    + " GAV versions are ignored if provided.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

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
    public Validated validate() {
        Validated validated = Validated.none();
        if (except != null) {
            for (int i = 0; i < except.size(); i++) {
                final String retainVersion = except.get(i);
                validated = validated.and(Validated.test(
                        String.format("except[%d]", i),
                        "did not look like a two-or-three-part GAV",
                        retainVersion,
                        maybeGav -> {
                            final int gavParts = maybeGav.split(":").length;
                            return gavParts == 2 || gavParts == 3;
                        }));
            }
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!isManagedDependencyTag()) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null && matchesVersion(d) &&
                            matchesGroup(d) && matchesArtifact(d)
                            && isNotExcepted(d)) {
                        Xml.Tag version = tag.getChild("version").orElse(null);
                        return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean matchesGroup(ResolvedDependency d) {
                return StringUtils.isNullOrEmpty(groupPattern) || matchesGlob(d.getGroupId(), groupPattern);
            }

            private boolean matchesArtifact(ResolvedDependency d) {
                return StringUtils.isNullOrEmpty(artifactPattern) || matchesGlob(d.getArtifactId(), artifactPattern);
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

            private boolean isNotExcepted(ResolvedDependency d) {
                if (except == null) {
                    return true;
                }
                for (final String gav : except) {
                    final String[] split = gav.split(":");
                    final String exceptedGroupId = split[0];
                    final String exceptedArtifactId = split[1];
                    if (matchesGlob(d.getGroupId(), exceptedGroupId)
                            && matchesGlob(d.getArtifactId(), exceptedArtifactId)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
