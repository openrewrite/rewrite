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
import org.openrewrite.javascript.internal.lock.npm.PosixPath;

import java.util.Map;

/**
 * The project's manifests by location relative to its root ({@code ""} the root, {@code packages/a} a member): what
 * yarn reads from the filesystem during an install.
 */
final class ProjectFiles {

    private final Map<String, String> manifests;

    ProjectFiles(Map<String, String> manifests) {
        this.manifests = manifests;
    }

    /**
     * The location a path relative to the project's root names, or null when it leaves the project.
     */
    static @Nullable String location(String... path) {
        String joined = PosixPath.join(path);
        if (".".equals(joined) || "./".equals(joined)) {
            return "";
        }
        if (PosixPath.isAbsolute(joined) || "..".equals(joined) || joined.startsWith("../")) {
            return null;
        }
        return joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }

    boolean hasManifest(@Nullable String location) {
        return location != null && manifests.containsKey(location);
    }

    @Nullable
    String manifest(String location) {
        return manifests.get(location);
    }

    Map<String, String> manifests() {
        return manifests;
    }
}
