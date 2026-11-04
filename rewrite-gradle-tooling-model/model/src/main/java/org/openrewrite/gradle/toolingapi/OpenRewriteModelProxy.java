/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.toolingapi;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface OpenRewriteModelProxy {
    byte[] getGradleProjectBytes();

    /**
     * The serialized {@link org.openrewrite.gradle.marker.GradleProject} for every project in the build, keyed by
     * its Gradle path (e.g. {@code ":"}, {@code ":sub"}). Allows consumers to attach the correct project model to a
     * subproject build file now that Gradle 9 no longer supports targeting an individual build file.
     * May be {@code null} when produced by an older version of the tooling plugin.
     */
    @Nullable Map<String, byte[]> getGradleProjectsByPathBytes();

    byte @Nullable [] getGradleSettingsBytes();
}
