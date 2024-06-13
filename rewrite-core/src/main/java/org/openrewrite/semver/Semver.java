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

import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.Scanner;
import java.util.regex.Pattern;

public class Semver {
    private Semver() {
    }

    public static boolean isVersion(String version) {
        return LatestRelease.RELEASE_PATTERN.matcher(version).matches();
    }

    public static Validated<VersionComparator> validate(String toVersion, @Nullable String metadataPattern) {
        return Validated.<VersionComparator, String>testNone(
                "metadataPattern",
                "must be a valid regular expression",
                metadataPattern, metadata -> {
                    try {
                        if (metadata != null) {
                            Pattern.compile(metadata);
                        }
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                }
        ).and(Validated.<VersionComparator>none()
                .or(LatestRelease.buildLatestRelease(toVersion, metadataPattern))
                .or(LatestIntegration.build(toVersion, metadataPattern))
                .or(LatestPatch.build(toVersion, metadataPattern))
                .or(HyphenRange.build(toVersion, metadataPattern))
                .or(XRange.build(toVersion, metadataPattern))
                .or(TildeRange.build(toVersion, metadataPattern))
                .or(CaretRange.build(toVersion, metadataPattern))
                .or(SetRange.build(toVersion, metadataPattern))
                .or(ExactVersion.build(toVersion))
        );
    }

    public static String majorVersion(String version) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        if (scanner.hasNext()) {
            return scanner.next();
        }
        return version;
    }

    public static String minorVersion(String version) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        if (scanner.hasNext()) {
            scanner.next();
        }
        if (scanner.hasNext()) {
            String minor = scanner.next();
            if (StringUtils.isNumeric(minor)) {
                return minor;
            }
        }
        return version;
    }
}
