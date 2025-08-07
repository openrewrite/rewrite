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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

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
 */
@Value
@Builder
public class GradleDependencyConstraint {
    String groupId;
    String artifactId;
    @Nullable
    String requiredVersion;
    @Nullable
    String preferredVersion;
    @Nullable
    String strictVersion;
    @Nullable
    String branch;
    @Nullable
    String reason;
    @Builder.Default
    List<String> rejectedVersions = emptyList();
}
