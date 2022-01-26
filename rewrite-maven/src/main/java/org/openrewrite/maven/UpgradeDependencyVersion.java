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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Upgrade the version of a dependency by specifying a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeDependencyVersion extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Trust parent POM",
            description = "Even if the parent suggests a version that is older than what we are trying to upgrade to, trust it anyway. " +
                    "Useful when you want to wait for the parent to catch up before upgrading. The parent is not trusted by default.",
            example = "false",
            required = false)
    @Nullable
    Boolean trustParent;

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
        return "Upgrade Maven dependency version";
    }

    @Override
    public String getDescription() {
        return "Upgrade the version of a dependency by specifying a group or group and artifact using Node Semver " +
                "advanced range selectors, allowing more precise control over version updates to patch or minor releases.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            Collection<String> availableVersions;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document m = super.visitDocument(document, ctx);

                for (MavenResolutionResult module : getResolutionResult().getModules()) {
                    String newerVersion = null;
                    String requestedVersion = null;

                    for (Dependency requestedDependency : module.getPom().getRequestedDependencies()) {
                        ResolvedDependency resolved = module.getResolvedDependency(requestedDependency);
                        if(resolved != null) {
                            if (matchesGlob(resolved.getGroupId(), groupId) && matchesGlob(resolved.getArtifactId(), artifactId)) {
                                newerVersion = findNewerVersion(resolved.getGroupId(), resolved.getArtifactId(),
                                        resolved.getVersion(), ctx);
                                requestedVersion = requestedDependency.getVersion();
                            }
                        }
                    }

                    if (newerVersion == null) {
                        for (DependencyManagementDependency dm : module.getPom().getRequested().getDependencyManagement()) {
                            if (matchesGlob(dm.getGroupId(), groupId) && matchesGlob(dm.getArtifactId(), artifactId)) {
                                //noinspection ConstantConditions
                                newerVersion = findNewerVersion(dm.getGroupId(), dm.getArtifactId(),
                                        module.getPom().getValue(dm.getVersion()), ctx);
                                requestedVersion = dm.getVersion();
                            }
                        }
                    }

                    if (newerVersion != null) {
                        assert requestedVersion != null;
                        if (requestedVersion.contains("${")) {
                            m = (Xml.Document) new ChangePropertyValue(requestedVersion, newVersion, false).getVisitor()
                                    .visitNonNull(m, ctx, getCursor());
                            break;
                        }
                    }
                }

                if (m != document) {
                    Pom requestedPom = getResolutionResult().getPom().getRequested();
                    doAfterVisit(new UpdateMavenModel());
                    doAfterVisit(new RemoveRedundantDependencyVersions());
                }

                return m;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if(isDependencyTag(groupId, artifactId)) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null) {
                        String newerVersion = findNewerVersion(d.getGroupId(), d.getArtifactId(), d.getVersion(), ctx);
                        if (newerVersion != null) {
                            Optional<Xml.Tag> version = t.getChild("version");
                            if (version.isPresent()) {
                                t = (Xml.Tag) new ChangeTagValueVisitor<Integer>(version.get(), newerVersion).visitNonNull(t, 0, getCursor());
                            }
                        }
                    }
                }
                return t;
            }

            @Nullable
            private String findNewerVersion(String groupId, String artifactId, String version, ExecutionContext ctx) {
                if (availableVersions == null) {
                    MavenMetadata mavenMetadata = downloadMetadata(groupId, artifactId, ctx);
                    if (mavenMetadata == null) {
                        availableVersions = emptyList();
                    } else {
                        availableVersions = new ArrayList<>();
                        for (String v : mavenMetadata.getVersioning().getVersions()) {
                            if (versionComparator.isValid(version, v)) {
                                availableVersions.add(v);
                            }
                        }
                    }
                }
                return versionComparator.upgrade(version, availableVersions).orElse(null);
            }
        };
    }
}
