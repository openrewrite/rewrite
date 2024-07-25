/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeManagedDependencyGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    // there are several implicitly defined version properties that we should never attempt to update
    private static final Set<String> implicitlyDefinedVersionProperties = new HashSet<>(Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    ));

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use.",
            example = "rewrite-testing-frameworks")
    String newArtifactId;

    @Option(displayName = "New version",
            description = "The new version to use.",
            example = "2.0.0",
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

    @Option(displayName = "Change property version names",
            description = "Allows property version names to be changed to best practice naming convention",
            example = "-jre",
            required = false)
    @Nullable
    Boolean changePropertyVersionNames;

    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId, @Nullable String newVersion) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, null, false);
    }

    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern, false);
    }

    @JsonCreator
    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean changePropertyVersionNames) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.changePropertyVersionNames = changePropertyVersionNames;
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated =
            validated.and(test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = isBlank(r.newGroupId) || Objects.equals(r.oldGroupId, r.newGroupId);
                    boolean sameArtifactId = isBlank(r.newArtifactId) || Objects.equals(r.oldArtifactId, r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
            ));
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Change Maven managed dependency groupId, artifactId and optionally the version";
    }

    @Override
    public String getDescription() {
        return "Change the groupId, artifactId and optionally the version of a specified Maven managed dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {

                Xml.Tag t = super.visitTag(tag, ctx);

                if (isManagedDependencyTag(oldGroupId, oldArtifactId)) {
                    Optional<Xml.Tag> groupIdTag = t.getChild("groupId");
                    boolean changed = false;
                    if (groupIdTag.isPresent() && !newGroupId.equals(groupIdTag.get().getValue().orElse(null))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(groupIdTag.get(), newGroupId));
                        changed = true;
                    }
                    Optional<Xml.Tag> artifactIdTag = t.getChild("artifactId");
                    if (artifactIdTag.isPresent() && !newArtifactId.equals(artifactIdTag.get().getValue().orElse(null))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(artifactIdTag.get(), newArtifactId));
                        changed = true;
                    }
                    Optional<Xml.Tag> versionTag = t.getChild("version");
                    if (versionTag.isPresent() && newVersion != null && !newVersion.equals(versionTag.get().getValue().orElse(null))) {
                        doAfterVisit(new ChangeVersionValue(newGroupId, newArtifactId, newVersion, versionPattern, ChangeVersionValue.Changes.MANAGED_DEPENDENCY.name()).getVisitor());
                        changed = true;
                    }
                    if (changed) {
                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null, (RemoveRedundantDependencyVersions.Comparator) null, null).getVisitor());
                    }
                }
                return t;
            }
        };
    }
}
