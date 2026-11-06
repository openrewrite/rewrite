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
 * What yarn reads from the filesystem during an install: the manifests by location ({@code ""} the root,
 * {@code ../lib} a directory above it), the paths the host holds when it says, how far above the project it holds
 * them, and the location yarn runs in.
 */
final class ProjectFiles {

    private final Map<String, String> manifests;
    private final @Nullable Set<String> paths;
    private final String cwd;
    private final @Nullable Integer reach;

    ProjectFiles(Map<String, String> manifests, @Nullable Set<String> paths) {
        this(manifests, paths, "", null);
    }

    /**
     * @param reach how many directories above the project's the host holds, the project's depth in the repository;
     *              null when the host has not said where the project is
     */
    ProjectFiles(Map<String, String> manifests, @Nullable Set<String> paths, String cwd, @Nullable Integer reach) {
        this.manifests = manifests;
        this.paths = paths;
        this.cwd = cwd;
        this.reach = reach;
    }

    String cwd() {
        return cwd;
    }

    /**
     * The location a path relative to the project's root names, {@code ../lib} above it; null for a path from the
     * filesystem's root.
     */
    static @Nullable String location(String... path) {
        String joined = PosixPath.join(path);
        if (".".equals(joined) || "./".equals(joined)) {
            return "";
        }
        if (PosixPath.isAbsolute(joined)) {
            return null;
        }
        return joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }

    /**
     * Whether a location lies above every directory the host holds, where the engine is given nothing.
     */
    boolean isOutside(String location) {
        int up = 0;
        for (String segment : location.split("/")) {
            if (!"..".equals(segment)) {
                break;
            }
            up++;
        }
        return up > (reach == null ? 0 : reach);
    }

    /**
     * What a location that {@link #isOutside} is outside of, as a failure words it.
     */
    String outside() {
        return reach == null ? "outside the project" : "outside the repository";
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
     * Whether the host says the project holds nothing at this location, neither a file nor a directory with files. Of
     * a directory above the project the host gives its package.json only, which does not say.
     */
    boolean isMissing(String location) {
        if (paths == null || manifests.containsKey(location) || "..".equals(location) ||
            location.startsWith("../")) {
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
