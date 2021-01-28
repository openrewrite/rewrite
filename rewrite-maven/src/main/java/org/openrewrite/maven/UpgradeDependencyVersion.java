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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.NoopCache;
import org.openrewrite.maven.internal.MavenDownloader;
import org.openrewrite.maven.internal.MavenMetadata;
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
@Data
@EqualsAndHashCode(callSuper = true)
public class UpgradeDependencyVersion extends Recipe {

    private final String groupId;

    @Nullable
    private final String artifactId;

    /**
     * Node Semver range syntax.
     */
    private final String newVersion;

    /**
     * Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     * The {@link HyphenRange} of "25-29" can be paired with a version pattern of "-jre" to select
     * Guava 29.0-jre
     */
    @Nullable
    private final String versionPattern;

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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeDependencyVersionVisitor();
    }


    private class UpgradeDependencyVersionVisitor extends MavenVisitor<ExecutionContext> {

        @Nullable
        private Collection<String> availableVersions;

        private VersionComparator versionComparator;

        public UpgradeDependencyVersionVisitor() {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(newVersion, versionPattern).getValue();
            setCursoringOn();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            settings = maven.getSettings();

            maybeChangeDependencyVersion(maven.getModel());

            for (Pom module : maven.getModules()) {
                maybeChangeDependencyVersion(module);
            }

            return super.visitMaven(maven, ctx);
        }

        private void maybeChangeDependencyVersion(Pom model) {
            for (Pom.Dependency dependency : model.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                    findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion()).ifPresent(newer -> {
                        ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion(groupId, dependency.getArtifactId(), newer);
                        doAfterVisit(changeDependencyVersion);
                    });
                }
            }

            for (DependencyManagementDependency dependency : model.getDependencyManagement().getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                    findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion()).ifPresent(newer -> {
                        ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion(groupId, dependency.getArtifactId(), newer);
                        doAfterVisit(changeDependencyVersion);
                    });
                }
            }
        }

        private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenDownloader(new NoopCache(), emptyMap(), settings)
                        .downloadMetadata(groupId, artifactId, emptyList());
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
