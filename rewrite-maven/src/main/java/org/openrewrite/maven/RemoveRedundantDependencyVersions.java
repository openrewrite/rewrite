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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestIntegration;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.emptyList;
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
            description = "Deprecated; use `onlyIfManagedVersionIs` instead. " +
                          "Only remove the explicit version if it exactly matches the managed dependency version. " +
                          "When `false` explicit versions will be removed if they are older than or equal to the managed dependency version. " +
                          "Default `true`.",
            required = false)
    @Nullable
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    Boolean onlyIfVersionsMatch;

    @Option(displayName = "Only if managed version is ...",
            description = "Only remove the explicit version if the managed version has the specified comparative relationship to the explicit version. " +
                          "For example, `gte` will only remove the explicit version if the managed version is the same or newer. " +
                          "Default `eq`.",
            valid = {"ANY", "EQ", "LT", "LTE", "GT", "GTE"},
            required = false)
    @Nullable
    Comparator onlyIfManagedVersionIs;

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs. Dependencies matching a GAV will be ignored by this recipe. " +
                          "GAV versions are ignored if provided.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

    public RemoveRedundantDependencyVersions(@Nullable String groupPattern, @Nullable String artifactPattern,
                                             @Nullable Comparator onlyIfManagedVersionIs, @Nullable List<String> except) {
        this(groupPattern, artifactPattern, null, onlyIfManagedVersionIs, except);
    }

    @JsonCreator
    private RemoveRedundantDependencyVersions(@Nullable String groupPattern, @Nullable String artifactPattern,
                                             @Nullable Boolean onlyIfVersionsMatch, @Nullable Comparator onlyIfManagedVersionIs,
                                             @Nullable List<String> except) {
        this.groupPattern = groupPattern;
        this.artifactPattern = artifactPattern;
        this.onlyIfVersionsMatch = onlyIfVersionsMatch;
        this.onlyIfManagedVersionIs = onlyIfManagedVersionIs;
        this.except = except;
    }

    public enum Comparator {
        ANY,
        EQ,
        LT,
        LTE,
        GT,
        GTE
    }

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
        if (onlyIfVersionsMatch != null && onlyIfManagedVersionIs != null) {
            validated = validated.and(Validated.invalid("onlyIfVersionsMatch", onlyIfVersionsMatch, "is deprecated in favor of onlyIfManagedVersionIs, and they cannot be used together"));
        }
        return validated;
    }

    private Comparator determineComparator() {
        if (onlyIfVersionsMatch != null) {
            return onlyIfVersionsMatch ? Comparator.EQ : Comparator.GTE;
        }
        if (onlyIfManagedVersionIs != null) {
            return onlyIfManagedVersionIs;
        }
        return Comparator.EQ;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Comparator comparator = determineComparator();
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (d != document) {
                    d = (Xml.Document) new RemoveEmptyDependenciesTags().visitNonNull(d, ctx);
                    d = (Xml.Document) new RemoveEmptyPluginsTags().visitNonNull(d, ctx);
                    if (comparator != Comparator.EQ) {
                        maybeUpdateModel();
                    }
                }
                return d;
            }

            @Override
            public  Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag()) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null &&
                            matchesGroup(d) &&
                            matchesArtifact(d) &&
                            matchesVersion(d) &&
                            isNotExcepted(d.getGroupId(), d.getArtifactId())) {
                        Xml.Tag version = tag.getChild("version").orElse(null);
                        return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                    }
                } else if (isManagedDependencyTag()) {
                    ResolvedManagedDependency managed = findManagedDependency(tag);
                    if (managed != null &&
                            matchesGroup(managed) &&
                            matchesArtifact(managed) &&
                            matchesVersion(managed, ctx) &&
                            isNotExcepted(managed.getGroupId(), managed.getArtifactId())) {
                        if (tag.getChild("exclusions").isPresent()) {
                            return tag;
                        }
                        return null;
                    }
                } else if (isPluginTag()) {
                    if (isManagedPluginTag()) {
                        Xml.Tag version = tag.getChild("version").orElse(null);
                        if (version == null) {
                            // version is not managed here
                            return tag;
                        }
                        Plugin p = findManagedPlugin(tag);
                        if (p != null && matchesGroup(p) && matchesArtifact(p) && matchesManagedVersion(p, ctx)) {
                            Set<String> gavTags = new HashSet<>(Arrays.asList("groupId", "artifactId", "version"));
                            if (tag.getChildren().stream().allMatch(t -> gavTags.contains(t.getName()))) {
                                // only the version was specified for this managed plugin, so no need to keep the declaration
                                return null;
                            }
                            // some other element is also declared (executions, configuration, dependencies…), so just remove the version
                            return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                        }
                    } else {
                        Plugin p = findPlugin(tag);
                        if (p != null && matchesGroup(p) && matchesArtifact(p) && matchesVersion(p)) {
                            Xml.Tag version = tag.getChild("version").orElse(null);
                            return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                        }
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
                if (d.getRequested().getVersion() == null || mrr.getPom().getRequested().getParent() == null) {
                    return false;
                }
                try {
                    GroupArtifactVersion parentGav = mrr.getPom().getRequested().getParent().getGav();
                    MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, mrr.getMavenSettings(), mrr.getActiveProfiles());
                    ResolvedPom parentPom = mpd.download(parentGav, null, mrr.getPom(), mrr.getPom().getRepositories())
                            .resolve(emptyList(), mpd, ctx);
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
                    return matchesComparator(versionAccordingToParent, d.getRequested().getVersion());
                } catch (Exception e) {
                    return false;
                }
            }

            private boolean matchesVersion(ResolvedDependency d) {
                if (d.getRequested().getVersion() == null) {
                    return false;
                }
                String managedVersion = getResolutionResult().getPom().getManagedVersion(d.getGroupId(),
                        d.getArtifactId(), d.getRequested().getType(), d.getRequested().getClassifier());
                return matchesComparator(managedVersion, d.getRequested().getVersion());
            }

            private boolean matchesVersion(Plugin p) {
                if (p.getVersion() == null) {
                    return false;
                }
                String managedVersion = getManagedPluginVersion(getResolutionResult().getPom(), p.getGroupId(), p.getArtifactId());
                return matchesComparator(managedVersion, p.getVersion());
            }


            /**
             * This compares a managed plugin version to the version which would be used if only the parent's
             * plugin management were in effect. This enables detection of managed plugin versions which
             * could be left to the parent.
             */
            private boolean matchesManagedVersion(Plugin p, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                if (p.getVersion() == null || mrr.getPom().getRequested().getParent() == null) {
                    return false;
                }
                try {
                    GroupArtifactVersion parentGav = mrr.getPom().getRequested().getParent().getGav();
                    MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, mrr.getMavenSettings(), mrr.getActiveProfiles());
                    ResolvedPom parentPom = mpd.download(parentGav, null, mrr.getPom(), mrr.getPom().getRepositories())
                            .resolve(emptyList(), mpd, ctx);
                    return parentPom.getPluginManagement().stream()
                            .filter(plugin -> plugin.getGroupId().equals(p.getGroupId()) && plugin.getArtifactId().equals(p.getArtifactId()))
                            .findFirst()
                            .map(Plugin::getVersion)
                            .map(versionAccordingToParent -> matchesComparator(parentPom.getValue(versionAccordingToParent), p.getVersion()))
                            .orElse(false);
                } catch (Exception e) {
                    return false;
                }
            }

            private boolean matchesComparator(@Nullable String managedVersion, String requestedVersion) {
                if (managedVersion == null) {
                    return false;
                }
                if (comparator == Comparator.ANY) {
                    return true;
                }
                if (!isExact(managedVersion)) {
                    return false;
                }
                int comparison = new LatestIntegration(null)
                        .compare(null, managedVersion,
                                Objects.requireNonNull(getResolutionResult().getPom().getValue(requestedVersion)));
                if (comparison < 0) {
                    return comparator == Comparator.LT || comparator == Comparator.LTE;
                } else if (comparison > 0) {
                    return comparator == Comparator.GT || comparator == Comparator.GTE;
                } else {
                    return comparator == Comparator.EQ || comparator == Comparator.LTE || comparator == Comparator.GTE;
                }
            }

            private boolean isExact(String managedVersion) {
                Validated<VersionComparator> maybeVersionComparator = Semver.validate(managedVersion, null);
                return maybeVersionComparator.isValid() && maybeVersionComparator.getValue() instanceof ExactVersion;
            }

            private boolean isNotExcepted(String groupId, String artifactId) {
                if (except == null) {
                    return true;
                }
                for (final String gav : except) {
                    String[] split = gav.split(":");
                    String exceptedGroupId = split[0];
                    String exceptedArtifactId = split[1];
                    if (matchesGlob(groupId, exceptedGroupId) &&
                        matchesGlob(artifactId, exceptedArtifactId)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private static @Nullable String getManagedPluginVersion(ResolvedPom resolvedPom, String groupId, String artifactId) {
        for (Plugin p : ListUtils.concatAll(resolvedPom.getPluginManagement(), resolvedPom.getRequested().getPluginManagement())) {
            if (Objects.equals(p.getGroupId(), groupId) && Objects.equals(p.getArtifactId(), artifactId)) {
                return resolvedPom.getValue(p.getVersion());
            }
        }
        return null;
    }

    private static class RemoveEmptyDependenciesTags extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public  Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (("dependencyManagement".equals(t.getName()) || "dependencies".equals(t.getName())) && (t.getContent() == null || t.getContent().isEmpty())) {
                return null;
            }
            return t;
        }
    }

    private static class RemoveEmptyPluginsTags extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public  Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (("pluginManagement".equals(t.getName()) || "plugins".equals(t.getName())) && (t.getContent() == null || t.getContent().isEmpty())) {
                return null;
            }
            return t;
        }
    }
}
