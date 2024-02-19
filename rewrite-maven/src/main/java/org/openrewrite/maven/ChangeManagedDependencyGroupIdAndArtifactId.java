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
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeManagedDependencyGroupIdAndArtifactId extends AbstractChangeDependencyGroupIdAndArtifactId {
    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId, @Nullable String newVersion) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, null);
    }

    @JsonCreator
    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        super(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern);
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
        return new AbstractChangeDependencyGroupIdAndArtifactIdVisitor() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {

                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);

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
                    if (newVersion != null) {
                        try {
                            Optional<Xml.Tag> versionTag = t.getChild("version");

                            if (versionTag.isPresent()) {
                                String resolvedNewVersion = resolveSemverVersion(ctx, newGroupId, newArtifactId, versionTag.get().getValue().orElse(null));
                                t = (Xml.Tag) new ChangeTagValueVisitor<>(versionTag.get(), resolvedNewVersion).visitNonNull(t, 0, getCursor().getParentOrThrow());
                            }
                            changed = true;
                        } catch(MavenDownloadingException e) {
                            return e.warn(t);
                        }
                    }
                    if (changed) {
                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null, null, null).getVisitor());
                    }
                }
                return t;
            }

            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId, @Nullable String currentVersion) throws MavenDownloadingException {
                if (versionComparator == null) {
                    return newVersion;
                }
                String finalCurrentVersion = currentVersion != null ? currentVersion : newVersion;
                if (availableVersions == null) {
                    availableVersions = new ArrayList<>();
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(finalCurrentVersion, v)) {
                            availableVersions.add(v);
                        }
                    }

                }
                return availableVersions.isEmpty() ? newVersion : Collections.max(availableVersions, versionComparator);
            }
        };
    }
}
