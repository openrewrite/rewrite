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

import lombok.Getter;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;

import java.util.Collection;
import java.util.Optional;

@With
@Getter
public class DependencyMatcher {

    private final @Nullable String groupPattern;
    private final @Nullable String artifactPattern;

    @Nullable
    private final VersionComparator versionComparator;

    public DependencyMatcher(@Nullable String groupPattern, @Nullable String artifactPattern, @Nullable VersionComparator versionComparator) {
        this.groupPattern = groupPattern;
        this.artifactPattern = artifactPattern;
        this.versionComparator = versionComparator;
    }

    public static Validated<DependencyMatcher> build(String pattern) {
        String[] patternPieces = pattern.split(":");
        if(patternPieces.length < 2) {
            return Validated.invalid("pattern", pattern, "missing required components. Must specify at least groupId:artifactId");
        } else if(patternPieces.length > 3) {
            return Validated.invalid("pattern", pattern, "not a valid pattern. Valid patterns take the form groupId:artifactId, groupId:artifactId:version, or groupId:artifactId:version/versionPattern");
        }
        if(patternPieces.length < 3) {
            return Validated.valid("pattern", new DependencyMatcher(patternPieces[0], patternPieces[1], null));
        }
        Validated<? extends VersionComparator> validatedVersion;

        if (patternPieces[2].contains("/")) {
            String[] versionPieces = patternPieces[2].split("/");
            if(versionPieces.length != 2) {
                return Validated.invalid("pattern", pattern, "unable to parse version \"" + patternPieces[2] + "\"");
            }
            validatedVersion = Semver.validate(versionPieces[0], versionPieces[1]);
        } else {
            validatedVersion = Semver.validate(patternPieces[2], null);
        }
        if(validatedVersion.isInvalid()) {
            return Validated.invalid("pattern", null, "Unable to parse version");
        }
        return Validated.valid("pattern", new DependencyMatcher(patternPieces[0], patternPieces[1], validatedVersion.getValue()));
    }

    public boolean matches(@Nullable String groupId, @Nullable String artifactId, String version) {
        return StringUtils.matchesGlob(groupId, groupPattern) &&
                StringUtils.matchesGlob(artifactId, artifactPattern) &&
                (versionComparator == null || versionComparator.isValid(null, version));
    }

    public boolean matches(@Nullable String groupId, @Nullable String artifactId) {
        return StringUtils.matchesGlob(groupId, groupPattern) && StringUtils.matchesGlob(artifactId, artifactPattern);
    }

    public boolean isValidVersion(@Nullable String currentVersion, String newVersion) {
        return versionComparator == null || versionComparator.isValid(currentVersion, newVersion);
    }

    public Optional<String> upgrade(String currentVersion, Collection<String> availableVersions) {
        if(versionComparator == null) {
            return Optional.empty();
        }
        return versionComparator.upgrade(currentVersion, availableVersions);
    }
}
