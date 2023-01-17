/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.utilities.RetainVersions;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeParentPom extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Override
    public String getDisplayName() {
        return "Change Maven Parent Pom";
    }

    @Override
    public String getDescription() {
        return "Change the parent pom of a Maven pom.xml. Identifies the parent pom to be changed by its groupId and artifactId.";
    }

    @Option(displayName = "Old GroupId",
            description = "The groupId of the maven parent pom to be changed away from.",
            example = "org.springframework.boot")
    String oldGroupId;

    @Option(displayName = "New GroupId",
            description = "The groupId of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldGroupId`.",
            example = "org.springframework.boot",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "Old ArtifactId",
            description = "The artifactId of the maven parent pom to be changed away from.",
            example = "spring-boot-starter-parent")
    String oldArtifactId;

    @Option(displayName = "New ArtifactId",
            description = "The artifactId of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldArtifactId`.",
            example = "spring-boot-starter-parent",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Allow version downgrades",
            description = "If the new parent has the same group/artifact, this flag can be used to only upgrade the " +
                          "version if the target version is newer than the current.",
            example = "false",
            required = false)
    @Nullable
    boolean allowVersionDowngrades;

    @Option(displayName = "Retain versions",
            description = "Accepts a list of GAVs. For each GAV, if it is a project direct dependency, and it is removed "
                    + "from dependency management in the new parent pom, then it will be retained with an explicit version. "
                    + "The version can be omitted from the GAV to use the old value from dependency management",
            example = "- com.jcraft:jsch",
            required = false)
    List<String> retainVersions;

    @Deprecated
    public ChangeParentPom(String oldGroupId, @Nullable String newGroupId, String oldArtifactId, @Nullable String newArtifactId, String newVersion, @Nullable String versionPattern, @Nullable Boolean allowVersionDowngrades) {
        this(oldGroupId, newGroupId, oldArtifactId, newArtifactId, newVersion, versionPattern, allowVersionDowngrades, null);
    }

    @JsonCreator
    public ChangeParentPom(String oldGroupId, @Nullable String newGroupId, String oldArtifactId, @Nullable String newArtifactId, String newVersion, @Nullable String versionPattern, @Nullable Boolean allowVersionDowngrades, @Nullable List<String> retainVersions) {
        this.oldGroupId = oldGroupId;
        this.newGroupId = newGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.allowVersionDowngrades = allowVersionDowngrades != null && allowVersionDowngrades;
        this.retainVersions = retainVersions == null ? new ArrayList<>() : retainVersions;
    }

    @Override
    protected MavenVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
                Parent parent = getResolutionResult().getPom().getRequested().getParent();
                if (parent != null &&
                    matchesGlob(parent.getArtifactId(), oldArtifactId) &&
                    matchesGlob(parent.getGroupId(), oldGroupId)) {
                    return SearchResult.found(document);
                }
                return document;
            }
        };
    }

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        for (int i = 0; i < retainVersions.size(); i++) {
            String retainVersion = retainVersions.get(i);
            validated = validated.and(Validated.test(
                    String.format("retainVersions[%d]", i),
                    "did not look like a two-or-three-part GAV",
                    retainVersion,
                    maybeGav -> {
                        int gavParts = maybeGav.split(":").length;
                        return gavParts == 2 || gavParts == 3;
                    }));
        }
        return validated;
    }

    @Override
    public MavenVisitor<ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            private Collection<String> availableVersions;

            @SuppressWarnings("OptionalGetWithoutIsPresent")
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isParentTag()) {
                    ResolvedPom resolvedPom = getResolutionResult().getPom();

                    if (matchesGlob(resolvedPom.getValue(tag.getChildValue("groupId").orElse(null)), oldGroupId) &&
                        matchesGlob(resolvedPom.getValue(tag.getChildValue("artifactId").orElse(null)), oldArtifactId)) {
                        String oldVersion = resolvedPom.getValue(tag.getChildValue("version").orElse(null));
                        assert oldVersion != null;
                        String targetGroupId = newGroupId == null ? tag.getChildValue("groupId").orElse(oldGroupId) : newGroupId;
                        String targetArtifactId = newArtifactId == null ? tag.getChildValue("artifactId").orElse(oldArtifactId) : newArtifactId;
                        try {
                            Optional<String> targetVersion = findNewerDependencyVersion(targetGroupId, targetArtifactId, oldVersion, ctx);
                            if (targetVersion.isPresent()) {
                                List<XmlVisitor<ExecutionContext>> changeParentTagVisitors = new ArrayList<>();
                                if (!oldGroupId.equals(targetGroupId)) {
                                    changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("groupId").get(), targetGroupId));
                                }

                                if (!oldArtifactId.equals(targetArtifactId)) {
                                    changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("artifactId").get(), targetArtifactId));
                                }

                                if (!oldVersion.equals(targetVersion.get())) {
                                    changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("version").get(), targetVersion.get()));
                                }

                                if (changeParentTagVisitors.size() > 0) {
                                    retainVersions();
                                    doAfterVisit(new RemoveRedundantDependencyVersions(null, null, true, retainVersions));
                                    for (XmlVisitor<ExecutionContext> visitor : changeParentTagVisitors) {
                                        doAfterVisit(visitor);
                                    }
                                    maybeUpdateModel();
                                    doAfterVisit(new RemoveRedundantDependencyVersions(null, null, true, null));
                                }
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                }
                return t;
            }

            private void retainVersions() {
                for (Recipe retainVersionRecipe : RetainVersions.plan(this, retainVersions)) {
                    doAfterVisit(retainVersionRecipe);
                }
            }

            private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion,
                                                                ExecutionContext ctx) throws MavenDownloadingException {
                if (availableVersions == null) {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(v -> versionComparator.isValid(currentVersion, v))
                            .filter(v -> allowVersionDowngrades || versionComparator.compare(currentVersion, currentVersion, v) < 0)
                            .collect(Collectors.toList());
                }
                return allowVersionDowngrades ? availableVersions.stream().max((v1, v2) -> versionComparator.compare(currentVersion, v1, v2)) : versionComparator.upgrade(currentVersion, availableVersions);
            }
        };
    }
}

