/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.regex.Matcher;

public class LatestIntegration extends LatestRelease {

    @Nullable
    String metadataPattern;

    public LatestIntegration(@Nullable String metadataPattern) {
        super(metadataPattern);
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        Matcher matcher = VersionComparator.RELEASE_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return false;
        }
        boolean requireMeta = !StringUtils.isNullOrEmpty(metadataPattern);
        String versionMeta = matcher.group(6);
        if (requireMeta) {
            return versionMeta != null && versionMeta.matches(metadataPattern);
        } else if (versionMeta == null) {
            return true;
        }
        String lowercaseVersionMeta = versionMeta.toLowerCase();
        for (String suffix : RELEASE_SUFFIXES) {
            if (suffix.equals(lowercaseVersionMeta)) {
                return true;
            }
        }
        return PRE_RELEASE_ENDING.matcher(lowercaseVersionMeta).matches();
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

    public static Validated<LatestIntegration> build(String toVersion, @Nullable String metadataPattern) {
        return ("latest.integration".equalsIgnoreCase(toVersion) || "latest.snapshot".equalsIgnoreCase(toVersion)) ?
                Validated.valid("latestIntegration", new LatestIntegration(metadataPattern)) :
                Validated.invalid("latestIntegration", toVersion, "not latest integration");
    }
}
