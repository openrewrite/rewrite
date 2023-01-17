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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Override managed version",
            description = "If the new dependency has a managed version, this flag can be used to explicitly set the version on the dependency. The default for this flag is `false`.",
            example = "false",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.overrideManagedVersion = false;
        this.versionPattern = versionPattern;
    }

    @JsonCreator
    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
    }

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
        return "Change Maven dependency groupId, artifactId and/or the version";
    }

    @Override
    public String getDescription() {
        return "Change the groupId, artifactId and/or the version of a specified Maven dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new MavenVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
            @Nullable
            private Collection<String> availableVersions;
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {

                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);

                if (isDependencyTag(oldGroupId, oldArtifactId)) {
                    String groupId = newGroupId;
                    if (groupId != null) {
                        t = changeChildTagValue(t, "groupId", groupId, ctx);
                    } else {
                        groupId = t.getChildValue("groupId").orElseThrow(NoSuchElementException::new);
                    }
                    String artifactId = newArtifactId;
                    if (artifactId != null) {
                        t = changeChildTagValue(t, "artifactId", artifactId, ctx);
                    } else {
                        artifactId = t.getChildValue("artifactId").orElseThrow(NoSuchElementException::new);
                    }
                    if (newVersion != null) {
                        try {
                            String resolvedNewVersion = resolveSemverVersion(ctx, groupId, artifactId);
                            Optional<Xml.Tag> scopeTag = t.getChild("scope");
                            Scope scope = scopeTag.map(xml -> Scope.fromName(xml.getValue().orElse("compile"))).orElse(Scope.Compile);
                            Optional<Xml.Tag> versionTag = t.getChild("version");
                            if (!versionTag.isPresent() &&
                                (Boolean.TRUE.equals(overrideManagedVersion) || !isDependencyManaged(scope, groupId, artifactId))) {
                                //If the version is not present, add the version if we are explicitly overriding a managed version or if no managed version exists.
                                Xml.Tag newVersionTag = Xml.Tag.build("<version>" + resolvedNewVersion + "</version>");
                                //noinspection ConstantConditions
                                t = (Xml.Tag) new AddToTagVisitor<ExecutionContext>(t, newVersionTag, new MavenTagInsertionComparator(t.getChildren())).visitNonNull(t, ctx, getCursor().getParent());
                            } else if (versionTag.isPresent()) {
                                if (isDependencyManaged(scope, groupId, artifactId) && !Boolean.TRUE.equals(overrideManagedVersion)) {
                                    //If the previous dependency had a version but the new artifact is managed, removed the
                                    //version tag.
                                    t = (Xml.Tag) new RemoveContentVisitor<>(versionTag.get(), false).visit(t, ctx);
                                } else {
                                    //Otherwise, change the version to the new value.
                                    t = changeChildTagValue(t, "version", resolvedNewVersion, ctx);
                                }
                            }
                        } catch(MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }

                //noinspection ConstantConditions
                return t;
            }

            private Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, String newValue, ExecutionContext ctx) {
                Optional<Xml.Tag> childTag = tag.getChild(childTagName);
                if (childTag.isPresent() && !newValue.equals(childTag.get().getValue().orElse(null))) {
                    tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag.get(), newValue).visitNonNull(tag, ctx);
                }
                return tag;
            }

            private boolean isDependencyManaged(Scope scope, String groupId, String artifactId) {

                MavenResolutionResult result = getResolutionResult();
                for (ResolvedManagedDependency managedDependency : result.getPom().getDependencyManagement()) {
                    if (groupId.equals(managedDependency.getGroupId()) && artifactId.equals(managedDependency.getArtifactId())) {
                        return scope.isInClasspathOf(managedDependency.getScope());
                    }
                }
                return false;
            }

            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId) throws MavenDownloadingException {
                if (versionComparator == null) {
                    return newVersion;
                }
                if (availableVersions == null) {
                    availableVersions = new ArrayList<>();
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(newVersion, v)) {
                            availableVersions.add(v);
                        }
                    }

                }
                return availableVersions.isEmpty() ? newVersion : Collections.max(availableVersions, versionComparator);
            }
        };
    }
}
