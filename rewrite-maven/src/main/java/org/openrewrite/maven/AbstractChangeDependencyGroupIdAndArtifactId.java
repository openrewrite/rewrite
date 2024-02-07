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

import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.utilities.MavenMetadataWrapper;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

abstract class AbstractChangeDependencyGroupIdAndArtifactId extends AbstractChangeGroupIdArtifactIdAndVersion {
    protected AbstractChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId,
            @Nullable String newVersion, @Nullable String versionPattern) {
        super(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern);
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated = addExtraValidation(validated);
        validated = validated.and(test(
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

    @NotNull
    protected Validated<Object> addExtraValidation(Validated<Object> validated) {
        return validated;
    }

    protected abstract class AbstractChangeDependencyGroupIdAndArtifactIdVisitor extends AbstractChangeGroupIdAndArtifactIdVisitor {
        @Nullable
        final VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
        @Nullable
        private Collection<String> availableVersions;

        @SuppressWarnings("ConstantConditions")
        protected String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId) throws MavenDownloadingException {
            if (versionComparator == null) {
                return newVersion;
            }
            if (availableVersions == null) {
				availableVersions = MavenMetadataWrapper.builder()
                        .mavenMetadata(metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx)))
                        .versionComparator(versionComparator)
                        .version(newVersion)
                        .build().filter();
            }
            return availableVersions.isEmpty() ? newVersion : Collections.max(availableVersions, versionComparator);
        }
    }
}