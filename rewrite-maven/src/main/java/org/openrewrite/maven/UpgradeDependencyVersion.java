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

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;

/**
 * Upgrade the version a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
public class UpgradeDependencyVersion extends MavenRefactorVisitor {
    private String groupId;

    @Nullable
    private String artifactId;

    /**
     * Node Semver range syntax.
     */
    private String toVersion;

    @Nullable
    private String metadataPattern;

    private VersionComparator versionComparator;

    public UpgradeDependencyVersion() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(@Nullable String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    /**
     * Allows us to extend version selection beyond the original Node Semver semantics. So for example,
     * We can pair a {@link HyphenRange} of "25-29" with a metadata pattern of "-jre" to select
     * Guava 29.0-jre
     *
     * @param metadataPattern The metadata pattern extending semver selection.
     */
    public void setMetadataPattern(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("toVersion", toVersion))
                .and(test("metadataPattern", "must be a valid regular expression",
                        metadataPattern, metadataPattern -> {
                            try {
                                if (metadataPattern != null) {
                                    Pattern.compile(metadataPattern);
                                }
                                return true;
                            } catch (Throwable e) {
                                return false;
                            }
                        }))
                .and(Semver.validate(toVersion, metadataPattern));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
        return super.visitPom(pom);
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);

        if (groupId.equals(d.getGroupId()) && (artifactId == null || artifactId.equals(d.getArtifactId())) &&
                !Maven.getPropertyKey(d.getVersion()).isPresent()) {
            Optional<String> newerVersion = maybeNewerVersion(d.getModel(),
                    d.getModel().getModuleVersion().getVersion());

            if (newerVersion.isPresent()) {
                ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion();
                changeDependencyVersion.setGroupId(groupId);
                changeDependencyVersion.setArtifactId(artifactId);
                changeDependencyVersion.setToVersion(newerVersion.get());
                andThen(changeDependencyVersion);
            }
        }

        return d;
    }

    @Override
    public Maven visitProperty(Maven.Property property) {
        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);

        property.findDependencies(pom, groupId, artifactId)
                .findAny()
                .flatMap(dependency -> maybeNewerVersion(dependency, property.getValue()))
                .ifPresent(newerVersion -> andThen(new ChangePropertyValue.Scoped(property, newerVersion)));

        return super.visitProperty(property);
    }

    @NotNull
    private Optional<String> maybeNewerVersion(MavenModel.Dependency d, String currentVersion) {
        LatestRelease latestRelease = new LatestRelease(metadataPattern);
        return d.getModuleVersion().getNewerVersions().stream()
                .filter(v -> versionComparator.isValid(v))
                .filter(v -> latestRelease.compare(currentVersion, v) < 0)
                .max(versionComparator);
    }
}
