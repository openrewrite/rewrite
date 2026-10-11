/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.List;

/**
 * Port of yarn src/package-constraint-resolver.js (yarn 1.22.22): the highest of the versions that satisfies a
 * range, or the last one listed when the range is still {@code latest} after the dist-tag lookup.
 */
final class PackageConstraintResolver {

    private final Config config;

    PackageConstraintResolver(Config config) {
        this.config = config;
    }

    @Nullable
    String reduce(List<String> versions, String range) {
        if ("latest".equals(range)) {
            return versions.isEmpty() ? null : versions.get(versions.size() - 1);
        }
        try {
            return NodeSemver.maxSatisfying(versions, range,
                    config.isLooseSemver() ? Config.LOOSE_SEMVER : Config.SEMVER);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
    }
}
