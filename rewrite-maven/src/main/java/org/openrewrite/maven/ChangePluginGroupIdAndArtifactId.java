/*
 * Copyright 2023 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.max;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old group ID",
            description = "The old group ID to replace. The group ID is the first part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifact ID",
            description = "The old artifactId to replace. The artifact ID is the second part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "my-deprecated-maven-plugin")
    String oldArtifactId;

    @Option(displayName = "New group ID",
            description = "The new group ID to use.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifact ID",
            description = "The new artifact ID to use.",
            example = "my-new-maven-plugin",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors)",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    String displayName = "Change Maven plugin group and artifact ID";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", newGroupId, newArtifactId);
    }

    String description = "Change the groupId and/or the artifactId of a specified Maven plugin. Optionally update the plugin version, " +
                "which may be given as an exact version or as a node-style semver selector resolved against the new plugin's available versions.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = newVersion == null ? null : Semver.validate(newVersion, null).getValue();

        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPluginTag(oldGroupId, oldArtifactId)) {
                    String resolvedVersion;
                    try {
                        resolvedVersion = resolveVersion(t, ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(tag);
                    }
                    if (newGroupId != null) {
                        t = changeChildTagValue(t, "groupId", newGroupId, ctx);
                    }
                    if (newArtifactId != null) {
                        t = changeChildTagValue(t, "artifactId", newArtifactId, ctx);
                    }
                    if (resolvedVersion != null) {
                        t = changeChildTagValue(t, "version", resolvedVersion, ctx);
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }
                //noinspection ConstantConditions
                return t;
            }

            private @Nullable String resolveVersion(Xml.Tag tag, ExecutionContext ctx) throws MavenDownloadingException {
                if (newVersion == null || versionComparator == null || versionComparator instanceof ExactVersion) {
                    return newVersion;
                }

                ResolvedPom resolvedPom = getResolutionResult().getPom();
                String groupId = resolvedPom.getValue(newGroupId != null ? newGroupId : tag.getChildValue("groupId").orElse(oldGroupId));
                String artifactId = resolvedPom.getValue(newArtifactId != null ? newArtifactId : tag.getChildValue("artifactId").orElse(oldArtifactId));
                if (groupId == null || artifactId == null) {
                    return null;
                }

                String currentVersion = tag.getChildValue("version").map(resolvedPom::getValue).orElse(null);
                MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadPluginMetadata(groupId, artifactId, ctx));
                List<String> availableVersions = new ArrayList<>();
                for (String v : mavenMetadata.getVersioning().getVersions()) {
                    if (versionComparator.isValid(currentVersion, v)) {
                        availableVersions.add(v);
                    }
                }
                return availableVersions.isEmpty() ? null : max(availableVersions, versionComparator);
            }
        };
    }
}
