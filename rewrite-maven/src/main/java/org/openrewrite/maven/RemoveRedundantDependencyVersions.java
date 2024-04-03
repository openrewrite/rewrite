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
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.LatestIntegration;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
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
            description = "Only remove the explicit version if it exactly matches the managed dependency version. " +
                          "When `false` explicit versions will be removed if they are older than or equal to the managed dependency version. " +
                          "Default `true`.",
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
        return "Remove redundant explicit dependency and plugin versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency/plugin versions when a parent POM's `dependencyManagement`/`pluginManagement` " +
               "specifies the version.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = Validated.none();
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
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (d != document) {
                    d = (Xml.Document) new RemoveEmptyDependencyTags().visitNonNull(d, ctx);
                    if (onlyIfVersionsMatch == null || !onlyIfVersionsMatch) {
                        maybeUpdateModel();
                    }
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag() || isPluginTag()) {
                    if (isPluginTag()) {
                        Plugin p = findPlugin(tag);
                        if (p != null && matchesGroup(p) && matchesArtifact(p) && matchesVersion(p)) {
                            Xml.Tag version = tag.getChild("version").orElse(null);
                            return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                        }
                    } else {
                        ResolvedDependency d = findDependency(tag);
                        if (d != null && matchesGroup(d) && matchesArtifact(d) && matchesVersion(d) && isNotExcepted(d)) {
                            Xml.Tag version = tag.getChild("version").orElse(null);
                            return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                        }
                    }
                } else if (isManagedDependencyTag()) {
                    ResolvedManagedDependency managed = findManagedDependency(tag);
                    if (managed != null && matchesGroup(managed) && matchesArtifact(managed) && matchesVersion(managed, ctx)) {
                        //noinspection DataFlowIssue
                        return null;
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean matchesGroup(ResolvedManagedDependency d) {
                return StringUtils.isNullOrEmpty(groupPattern) || matchesGlob(d.getGroupId(), groupPattern);
            }

            private boolean matchesGroup(ResolvedDependency d) {
                return StringUtils.isNullOrEmpty(groupPattern) || matchesGlob(d.getGroupId(), groupPattern);
            }

            private boolean matchesGroup(Plugin p) {
                return StringUtils.isNullOrEmpty(groupPattern) || matchesGlob(p.getGroupId(), groupPattern);
            }

            private boolean matchesArtifact(ResolvedManagedDependency d) {
                return StringUtils.isNullOrEmpty(artifactPattern) || matchesGlob(d.getArtifactId(), artifactPattern);
            }

            private boolean matchesArtifact(ResolvedDependency d) {
                return StringUtils.isNullOrEmpty(artifactPattern) || matchesGlob(d.getArtifactId(), artifactPattern);
            }

            private boolean matchesArtifact(Plugin p) {
                return StringUtils.isNullOrEmpty(artifactPattern) || matchesGlob(p.getArtifactId(), artifactPattern);
            }

            /**
             * This compares a managed dependency version to the version which would be used if only the parent's
             * dependency management were in effect. This enables detection of managed dependency versions which
             * could be left to the parent.
             */
            private boolean matchesVersion(ResolvedManagedDependency d, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                if (d.getRequested().getVersion() == null || d.getRequested().getVersion().contains("${") || mrr.getPom().getRequested().getParent() == null) {
                    return false;
                }
                try {
                    GroupArtifactVersion parentGav = mrr.getPom().getRequested().getParent().getGav();
                    MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, null, null);
                    ResolvedPom parentPom = mpd.download(parentGav, null, null, mrr.getPom().getRepositories())
                            .resolve(Collections.emptyList(), mpd, ctx);
                    ResolvedManagedDependency parentManagedVersion = parentPom.getDependencyManagement().stream()
                            .filter(dep -> dep.getGroupId().equals(d.getGroupId()) && dep.getArtifactId().equals(d.getArtifactId()))
                            .findFirst()
                            .orElse(null);
                    if (parentManagedVersion == null) {
                        return false;
                    }
                    String versionAccordingToParent = parentManagedVersion.getVersion();
                    if (versionAccordingToParent == null) {
                        return false;
                    }
                    if (isExactMatchRequired()) {
                        return Objects.equals(versionAccordingToParent, d.getRequested().getVersion());
                    }
                    return isManagedNewerThanRequested(versionAccordingToParent, d.getRequested().getVersion());
                } catch (Exception e) {
                    return false;
                }
            }

            private boolean matchesVersion(ResolvedDependency d) {
                if (d.getRequested().getVersion() == null || d.getRequested().getVersion().contains("${")) {
                    return false;
                }
                String managedVersion = getResolutionResult().getPom().getManagedVersion(d.getGroupId(),
                        d.getArtifactId(), d.getRequested().getType(), d.getRequested().getClassifier());
                if (isExactMatchRequired()) {
                    return Objects.equals(managedVersion, d.getRequested().getVersion());
                }
                return isManagedNewerThanRequested(managedVersion, d.getRequested().getVersion());
            }

            private boolean matchesVersion(Plugin p) {
                if (p.getVersion() == null || p.getVersion().contains("${")) {
                    return false;
                }
                String managedVersion = getManagedPluginVersion(getResolutionResult().getPom(), p.getGroupId(), p.getArtifactId());
                if (isExactMatchRequired()) {
                    return Objects.equals(managedVersion, p.getVersion());
                }
                return isManagedNewerThanRequested(managedVersion, p.getVersion());
            }

            private boolean isManagedNewerThanRequested(@Nullable String managedVersion, String requestedVersion) {
                return managedVersion != null && new LatestIntegration(null)
                                                         .compare(null, managedVersion, requestedVersion) >= 0;
            }

            private boolean isExactMatchRequired() {
                return onlyIfVersionsMatch == null || onlyIfVersionsMatch;
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

    @Nullable
    private static String getManagedPluginVersion(ResolvedPom resolvedPom, @Nullable String groupId, String artifactId) {
        for (Plugin p : resolvedPom.getPluginManagement()) {
            if (Objects.equals(
                    Optional.ofNullable(p.getGroupId()).orElse("org.apache.maven.plugins"),
                    Optional.ofNullable(groupId).orElse("org.apache.maven.plugins")) &&
                Objects.equals(p.getArtifactId(), artifactId)) {
                return resolvedPom.getValue(p.getVersion());
            }
        }
        return null;
    }

    private static class RemoveEmptyDependencyTags extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (("dependencyManagement".equals(t.getName()) || "dependencies".equals(t.getName())) && (t.getContent() == null || t.getContent().isEmpty())) {
                //noinspection DataFlowIssue
                return null;
            }
            return t;
        }
    }
}
