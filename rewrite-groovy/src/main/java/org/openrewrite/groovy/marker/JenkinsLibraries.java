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
package org.openrewrite.groovy.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.util.List;
import java.util.UUID;

/// The Jenkins shared libraries a Jenkinsfile loads, through `@Library` annotations or the `library` step.
///
/// Only literal library identifiers are recorded, so `library "my-lib@${branch}"` is left out.
/// Calls to the steps these libraries provide remain unresolved; this marker records which libraries
/// they may come from.
@Value
@With
public class JenkinsLibraries implements Marker {
    UUID id;
    List<Library> libraries;

    @Value
    public static class Library {
        String name;

        /// The version, branch or tag after the `@` in the library identifier, or `null` to use the
        /// default version configured in Jenkins.
        @Nullable
        String version;
    }
}
