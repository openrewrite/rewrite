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
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.ManifestRules;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Yarn classic's reading of the files the dependency recipes edit: {@code resolutions} matched by package names from
 * the root, the first key of a glob winning, and its add, which keeps an empty dependency field.
 */
public final class YarnClassicManifestRules implements ManifestRules {

    public static final YarnClassicManifestRules RULES = new YarnClassicManifestRules();

    private YarnClassicManifestRules() {
    }

    @Override
    public OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                       int release) {
        return OverridePlace.RESOLUTIONS;
    }

    // override-key-versioned-parent
    @Override
    public @Nullable String overrideDeclined(String packageName, List<DependencyPathSegment> path) {
        for (DependencyPathSegment segment : path) {
            if (segment.getVersion() != null) {
                return "yarn matches a resolution's path by package names only, not " + segment.getName() + "@" +
                       segment.getVersion();
            }
        }
        return null;
    }

    // matched from the root (small-resolutions-parent-not-root), the first key of the same glob being the one applied
    @Override
    public String overrideKey(String packageName, List<DependencyPathSegment> path, List<String> existing) {
        StringBuilder key = new StringBuilder(path.isEmpty() ? "" : "**/");
        for (DependencyPathSegment segment : path) {
            key.append(segment.getName()).append('/');
        }
        String written = key.append(packageName).toString();
        String glob = ResolutionMap.globPattern(written);
        for (String other : existing) {
            if (glob != null && glob.equals(ResolutionMap.globPattern(other))) {
                return other;
            }
        }
        return written;
    }

    @Override
    public @Nullable String outranked(String packageName, List<String> keys,
                                      Map<String, @Nullable Object> resolutions) {
        List<String> before = ResolutionMap.triedBefore(resolutions, overrideKey(packageName, emptyList(), keys));
        return before.isEmpty() ? null : "yarn applies " + String.join(", ", before) +
                                         " before a global override of " + packageName;
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return DEPENDENCY_FIELDS;
    }
}
