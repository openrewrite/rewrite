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
package org.openrewrite.gradle.marker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Models a dependency constraint in Gradle. These are typically used to manage the versions of transitive dependencies.
 * For example:
 * <pre>
 *     dependencies {
 *         constraints {
 *              implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")
 *         }
 *     }
 * </pre>
 * Compare to Gradle's internal org.gradle.api.internal.artifacts.dependencies.DefaultDependencyConstraint
 * See: <a href="https://docs.gradle.org/current/userguide/dependency_versions.html#sec:rich-version-constraints">rich version constraints</a>.
 */
@Value
@With
@Builder
@AllArgsConstructor
public class GradleDependencyConstraint implements Serializable {
    String groupId;
    String artifactId;
    /**
     * Sets a minimum version, allowing newer versions to be selected in conflict resolution.
     * Supports dynamic version selectors like "1.+".
     */
    @Nullable
    String requiredVersion;
    /**
     * Lowest-precedence version selected if nothing else otherwise specifies the version.
     * Does not support dynamic versions like "1.+", Must be a literal, individual version number.
     */
    @Nullable
    String preferredVersion;
    /**
     * Ensure that only the specified version of a dependency is used, rejecting any other versions even if they are newer.
     * Supports dynamic version selectors like "1.+".
     */
    @Nullable
    String strictVersion;

    @Nullable
    String branch;

    @Nullable
    String reason;

    @Builder.Default
    List<String> rejectedVersions = emptyList();

    /**
     * Attempt to boil down the several version numbers/patterns into a single version number.
     * Potentially lossy as the exact version selection process requires a list of available versions.
     * But in situations where you know that complex selectors are not involved it's convenient to grab the highest-precedence
     * number available.
     */
    public String approximateEffectiveVersion() {
        //TODO: Parse patterns rather than assuming only literal version numbers are in effect
        return strictVersion != null ? strictVersion :
                requiredVersion != null ? requiredVersion :
                        preferredVersion != null ? preferredVersion : "";
    }
}
