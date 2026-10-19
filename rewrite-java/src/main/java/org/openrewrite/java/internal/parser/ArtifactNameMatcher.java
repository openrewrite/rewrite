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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Pattern;

/// Matches the artifact names passed to `classpathFromResources`, such as `guava`, `guava-31` or
/// `log4j-api-2.+`, against `artifactId-version` strings.
///
/// The name is a pattern prefix, so `netty-transport` also matches `netty-transport-classes-epoll-4.2.19.Final`.
/// When several candidates match, the one whose artifactId the name covers wins, so that the result does not
/// depend on candidate order.
final class ArtifactNameMatcher {
    private final String artifactName;
    private final Pattern pattern;

    ArtifactNameMatcher(String artifactName) {
        this.artifactName = artifactName;
        this.pattern = Pattern.compile(artifactName + ".*");
    }

    boolean matches(String artifactIdAndVersion) {
        return pattern.matcher(artifactIdAndVersion).matches();
    }

    /// Whether the name is the artifactId itself, optionally followed by a version prefix,
    /// rather than a prefix of a longer artifactId.
    private boolean covers(String artifactId) {
        return artifactName.equals(artifactId) || artifactName.startsWith(artifactId + "-");
    }

    <T> @Nullable T select(Iterable<T> candidates, Function<T, String> artifactId,
                           Function<T, String> artifactIdAndVersion, boolean coveredOnly) {
        T fallback = null;
        for (T candidate : candidates) {
            if (matches(artifactIdAndVersion.apply(candidate))) {
                if (covers(artifactId.apply(candidate))) {
                    return candidate;
                }
                if (fallback == null && !coveredOnly) {
                    fallback = candidate;
                }
            }
        }
        return fallback;
    }
}
