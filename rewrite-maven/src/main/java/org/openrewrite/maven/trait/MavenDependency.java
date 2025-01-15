/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven.trait;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestPatch;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class MavenDependency implements Trait<Xml.Tag> {
    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    private static final XPathMatcher PROFILE_DEPENDENCY_MATCHER = new XPathMatcher("/project/profiles/profile/dependencies/dependency");

    Cursor cursor;

    @Getter
    ResolvedDependency resolvedDependency;

    public static @Nullable String findNewerVersion(
            String groupId,
            String artifactId,
            @Nullable String currentVersion,
            MavenResolutionResult mrr,
            MavenMetadataFailures metadataFailures,
            VersionComparator versionComparator,
            ExecutionContext ctx) throws MavenDownloadingException {
        String finalVersion = !Semver.isVersion(currentVersion) ? "0.0.0" : currentVersion;

        // in the case of "latest.patch", a new version can only be derived if the
        // current version is a semantic version
        if (versionComparator instanceof LatestPatch && !versionComparator.isValid(finalVersion, finalVersion)) {
            return null;
        }

        try {
            MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
            MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> new MavenPomDownloader(
                    emptyMap(), ctx,
                    Optional.ofNullable(mctx.getSettings())
                            .orElse(mrr.getMavenSettings()),
                    Optional.ofNullable(mctx.getSettings())
                            .map(MavenSettings::getActiveProfiles)
                            .map(MavenSettings.ActiveProfiles::getActiveProfiles)
                            .orElse(mrr.getActiveProfiles()))
                    .downloadMetadata(new GroupArtifact(groupId, artifactId), null, mrr.getPom().getRepositories()));
            List<String> versions = new ArrayList<>();
            for (String v : mavenMetadata.getVersioning().getVersions()) {
                if (versionComparator.isValid(finalVersion, v)) {
                    versions.add(v);
                }
            }

            // Some repositories will have corrupt or incomplete maven metadata which
            // prevents an upgrade even to a fixed version. For example this metadata file is missing all versions after 2019:
            // https://repository.mapr.com/nexus/content/groups/mapr-public/org/apache/hbase/hbase-annotations/maven-metadata.xml
            if (versionComparator instanceof ExactVersion) {
                String exactVersion = ((ExactVersion) versionComparator).getVersion();
                if (!versions.contains(exactVersion)) {
                    try {
                        // This is a best effort attempt to see if the pom is there anyway, in spite of the
                        // fact that it's not in the metadata. Usually it won't be, only in situations like the
                        // MapR repository mentioned in the comment above will it be.
                        Pom pom = new MavenPomDownloader(emptyMap(), ctx,
                                mrr.getMavenSettings(), mrr.getActiveProfiles()).download(new GroupArtifactVersion(groupId, artifactId, ((ExactVersion) versionComparator).getVersion()),
                                null, null, mrr.getPom().getRepositories());
                        if (pom.getGav().getVersion().equals(exactVersion)) {
                            return exactVersion;
                        }
                    } catch (MavenDownloadingException e) {
                        return null;
                    }
                }
            }

            // handle upgrades from non semver versions like "org.springframework.cloud:spring-cloud-dependencies:Camden.SR5"
            if (!Semver.isVersion(finalVersion) && !versions.isEmpty()) {
                versions.sort(versionComparator);
                return versions.get(versions.size() - 1);
            }
            return versionComparator.upgrade(finalVersion, versions).orElse(null);
        } catch (IllegalStateException e) {
            // this can happen when we encounter exotic versions
            return null;
        }
    }

    public static class Matcher extends MavenTraitMatcher<MavenDependency> {
        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        protected @Nullable MavenDependency test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;

                // `XPathMatcher` is still a bit expensive
                if (!"dependency".equals(tag.getName()) ||
                    (!DEPENDENCY_MATCHER.matches(cursor) &&
                     !PROFILE_DEPENDENCY_MATCHER.matches(cursor))) {
                    return null;
                }

                Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult(cursor).getDependencies();
                for (Scope scope : Scope.values()) {
                    if (dependencies.containsKey(scope)) {
                        for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                            if ((groupId == null || matchesGlob(resolvedDependency.getGroupId(), groupId)) &&
                                (artifactId == null || matchesGlob(resolvedDependency.getArtifactId(), artifactId))) {
                                String scopeName = tag.getChildValue("scope").orElse(null);
                                Scope tagScope = scopeName != null ? Scope.fromName(scopeName) : null;
                                if (tagScope == null && artifactId != null) {
                                    tagScope = getResolutionResult(cursor).getPom().getManagedScope(
                                            groupId,
                                            artifactId,
                                            tag.getChildValue("type").orElse(null),
                                            tag.getChildValue("classifier").orElse(null)
                                    );
                                }
                                if (tagScope == null) {
                                    tagScope = Scope.Compile;
                                }
                                Dependency req = resolvedDependency.getRequested();
                                String reqGroup = req.getGroupId();
                                if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                                    req.getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)) &&
                                    scope == tagScope) {
                                    return new MavenDependency(cursor, resolvedDependency);
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }
}
