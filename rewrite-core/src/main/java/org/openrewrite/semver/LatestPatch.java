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
package org.openrewrite.semver;

import lombok.Value;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

@Value
public class LatestPatch implements VersionComparator {
    @Nullable
    String metadataPattern;

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        Validated<? extends VersionComparator> validated = currentVersion == null ?
                LatestRelease.buildLatestRelease("latest.release", metadataPattern) :
                TildeRange.build("~" + Semver.majorVersion(currentVersion) + "." + Semver.minorVersion(currentVersion), metadataPattern);

        if (validated.isValid()) {
            VersionComparator comparator = validated.getValue();
            if (comparator != null) {
                return comparator.isValid(currentVersion, version);
            }
        }
        return false;
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        if(currentVersion == null) {
            return new LatestRelease(null)
                    .compare(null, v1, v2);
        }

        //noinspection ConstantConditions
        return TildeRange.build("~" + Semver.majorVersion(currentVersion) + "." + Semver.minorVersion(currentVersion), metadataPattern)
                .getValue()
                .compare(currentVersion, v1, v2);
    }

    public static Validated<LatestPatch> build(String toVersion, @Nullable String metadataPattern) {
        return "latest.patch".equalsIgnoreCase(toVersion) ?
                Validated.valid("latestPatch", new LatestPatch(metadataPattern)) :
                Validated.invalid("latestPatch", toVersion, "not latest release");
    }
}
