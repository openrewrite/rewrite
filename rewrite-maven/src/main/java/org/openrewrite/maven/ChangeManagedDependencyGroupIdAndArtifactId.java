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
import java.util.concurrent.atomic.AtomicBoolean;

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
					AtomicBoolean changed = new AtomicBoolean(false);
                    t.getChild("groupId").ifPresent(groupId -> {
                        if (!newGroupId.equals(groupId.getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(groupId, newGroupId));
                            changed.set(true);
                        }
                    });
					t.getChild("artifactId").ifPresent(artifactId -> {
                        if (!newArtifactId.equals(artifactId.getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(artifactId, newArtifactId));
                            changed.set(true);
                        }
                    });
                    if (newVersion != null) {
                        try {
                            Optional<Xml.Tag> versionTag = t.getChild("version");

                            if (versionTag.isPresent()) {
                                Xml.Tag versionTagValue = versionTag.get();
                                String resolvedNewVersion = resolveSemverVersion(ctx, newGroupId, newArtifactId, versionTagValue.getValue().orElse(null));
                                t = (Xml.Tag) new ChangeTagValueVisitor<>(versionTagValue, resolvedNewVersion).visitNonNull(t, 0, getCursor().getParentOrThrow());
                            }
                            changed.set(true);
                        } catch(MavenDownloadingException e) {
                            return e.warn(t);
                        }
                    }
                    if (changed.get()) {
                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null, null, null).getVisitor());
                    }
                }
                return t;
            }
        };
    }
}
