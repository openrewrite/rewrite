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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.DependencyManagementDependency;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Upgrade the version a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeDependencyVersion extends Recipe {

    String groupId;

    @Nullable
    String artifactId;

    /**
     * Node Semver range syntax.
     */
    String newVersion;

    /**
     * Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     * The {@link HyphenRange} of "25-29" can be paired with a version pattern of "-jre" to select
     * Guava 29.0-jre
     */
    @Nullable
    String versionPattern;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Dependency Version";
    }

    @Override
    public String getDescription() {
        return "Upgrade the version a group or group and artifact using Node Semver advanced range selectors, " +
                "allowing more precise control over version updates to patch or minor releases";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeDependencyVersionVisitor();
    }

    private class UpgradeDependencyVersionVisitor extends MavenVisitor {
        @Nullable
        private Collection<String> availableVersions;

        private final VersionComparator versionComparator;

        public UpgradeDependencyVersionVisitor() {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            maybeChangeDependencyVersion(maven.getModel(), ctx);

            for (Pom module : maven.getModules()) {
                maybeChangeDependencyVersion(module, ctx);
            }

            return super.visitMaven(maven, ctx);
        }

        private void maybeChangeDependencyVersion(Pom model, ExecutionContext ctx) {
            for (Pom.Dependency dependency : model.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                    findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion(), ctx).ifPresent(newer -> {
                        ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion(groupId, dependency.getArtifactId(), newer);
                        doAfterVisit(changeDependencyVersion);
                    });
                }
            }

            for (DependencyManagementDependency dependency : model.getDependencyManagement().getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                    findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion(), ctx).ifPresent(newer -> {
                        ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion(groupId, dependency.getArtifactId(), newer);
                        doAfterVisit(changeDependencyVersion);
                    });
                }
            }
        }

        private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion,
                                                            ExecutionContext ctx) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                        emptyMap(), ctx).downloadMetadata(groupId, artifactId, emptyList());
                availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                        .filter(versionComparator::isValid)
                        .collect(Collectors.toList());
            }

            LatestRelease latestRelease = new LatestRelease(versionPattern);
            return availableVersions.stream()
                    .filter(v -> latestRelease.compare(currentVersion, v) < 0)
                    .max(versionComparator);
        }
    }
}
