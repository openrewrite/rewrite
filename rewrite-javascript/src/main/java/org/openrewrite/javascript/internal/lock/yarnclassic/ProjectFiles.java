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
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.Map;
import java.util.Set;

/**
 * What yarn reads from the filesystem during an install: the manifests by location ({@code ""} the root), the paths the
 * project holds when the host says, and the location yarn runs in.
 */
final class ProjectFiles {

    private final Map<String, String> manifests;
    private final @Nullable Set<String> paths;
    private final String cwd;

    ProjectFiles(Map<String, String> manifests, @Nullable Set<String> paths) {
        this(manifests, paths, "");
    }

    ProjectFiles(Map<String, String> manifests, @Nullable Set<String> paths, String cwd) {
        this.manifests = manifests;
        this.paths = paths;
        this.cwd = cwd;
    }

    String cwd() {
        return cwd;
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

    /**
     * Whether the project holds no such file; false when the host has not said which paths it holds.
     */
    boolean isAbsent(String path) {
        return paths != null && !paths.contains(path);
    }

    /**
     * Whether the host says the project holds nothing at this location, neither a file nor a directory with files.
     */
    boolean isMissing(String location) {
        if (paths == null || manifests.containsKey(location)) {
            return false;
        }
        for (String path : paths) {
            if (path.equals(location) || path.startsWith(location + "/")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the host says the project holds this file.
     */
    boolean holds(String path) {
        return paths != null && paths.contains(path);
    }

    @Nullable
    String manifest(String location) {
        return manifests.get(location);
    }

    Map<String, String> manifests() {
        return manifests;
    }
}
