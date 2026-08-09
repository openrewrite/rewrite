/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradleWrapperProperties {
    private static final Pattern DISTRIBUTION_URL_VERSION = Pattern.compile("gradle-(\\d+(?:\\.\\d+)*(?:-[A-Za-z0-9][A-Za-z0-9.-]*)?)-(?:bin|all)\\.zip");

    private GradleWrapperProperties() {
    }

    /**
     * The Gradle version a wrapper declares, read from the {@code distributionUrl} it points at. Used when no
     * {@link org.openrewrite.marker.BuildTool} marker is available, which is the case for any parser that isn't the
     * OpenRewrite Gradle plugin. Tolerates custom and mirrored distribution hosts.
     *
     * @param distributionUrl the {@code distributionUrl} value, which may still carry the properties-file escaping of {@code :}
     * @return the version, or {@code null} when the URL contains no recognizable Gradle version
     */
    public static @Nullable String versionFromDistributionUrl(String distributionUrl) {
        Matcher matcher = DISTRIBUTION_URL_VERSION.matcher(distributionUrl.replace("\\", ""));
        return matcher.find() ? matcher.group(1) : null;
    }
}
