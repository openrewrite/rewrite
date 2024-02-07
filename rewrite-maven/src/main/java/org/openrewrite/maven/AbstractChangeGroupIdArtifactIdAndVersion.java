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

import org.openrewrite.Option;
import org.openrewrite.internal.lang.Nullable;

abstract class AbstractChangeGroupIdArtifactIdAndVersion extends AbstractChangeGroupIdAndArtifactId {
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

    protected AbstractChangeGroupIdArtifactIdAndVersion(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId,
            @Nullable String newVersion, @Nullable String versionPattern) {
        super(oldGroupId, oldArtifactId, newGroupId, newArtifactId);
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
    }
}