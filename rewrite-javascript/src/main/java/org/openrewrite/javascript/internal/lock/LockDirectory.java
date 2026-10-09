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
package org.openrewrite.javascript.internal.lock;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A regeneration's request seen from the lock's directory: the project's manifests, the package-manager files and the
 * paths the host holds, by path relative to that directory ({@code ../} for those outside it), and the directory the
 * package manager runs in, the edited manifest's.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class LockDirectory {

    /**
     * The lock's directory relative to the repository's root; {@code ""} is the root.
     */
    private final String path;

    @Nullable
    private final String lock;

    /**
     * The project's manifests as edited, by directory, the edited one last.
     */
    private final Map<String, String> manifests;

    /**
     * The package-manager files the host read and the package.json of the directories above, by path.
     */
    private final Map<String, String> files;

    /**
     * Every path the host holds of the project and the directories above it; null when it has not said.
     */
    @Nullable
    private final Set<String> paths;

    /**
     * The edited manifest's directory, where the package manager runs.
     */
    private final String runsIn;

    /**
     * The marker's project npmrc, which it read from the repository's root ({@code readNpmrcConfigs(relativeTo ||
     * dir)}), stands in for the root's .npmrc when the host did not read that file itself.
     */
    public static LockDirectory of(Request request) {
        Path edited = request.getPackageJsonPath() == null ? Paths.get("package.json") : request.getPackageJsonPath();
        String editedDir = directory(edited);
        String path = editedDir;
        for (Path manifest : request.getManifests().keySet()) {
            String candidate = directory(manifest);
            if (contains(candidate, editedDir) && candidate.length() < path.length()) {
                path = candidate;
            }
        }
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<Path, String> manifest : request.getManifests().entrySet()) {
            manifests.put(relative(path, directory(manifest.getKey())), manifest.getValue());
        }
        manifests.put(relative(path, editedDir), request.getPackageJson());

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            files.put(relative(path, normalize(file.getKey())), file.getValue());
        }
        String rootNpmrc = relative(path, ".npmrc");
        String markerNpmrc = projectNpmrc(request.getMarker());
        if (markerNpmrc != null && !files.containsKey(rootNpmrc)) {
            files.put(rootNpmrc, markerNpmrc);
        }

        Set<String> paths = null;
        if (request.getProjectPaths() != null) {
            paths = new LinkedHashSet<>();
            for (Path held : request.getProjectPaths()) {
                paths.add(relative(path, normalize(held)));
            }
        }
        return new LockDirectory(path, request.getLock(), manifests, files, paths, relative(path, editedDir));
    }

    /**
     * The name of the lock's directory; null at the repository's root, whose name the host does not know.
     */
    public @Nullable String name() {
        Path name = path.isEmpty() ? null : Paths.get(path).getFileName();
        return name == null ? null : name.toString();
    }

    public @Nullable String file(String relativePath) {
        return files.get(relativePath);
    }

    /**
     * The files under {@code dir}, a directory relative to the lock's, by path relative to it.
     */
    public Map<String, String> filesUnder(String dir) {
        Map<String, String> under = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            String relative = under(dir, file.getKey());
            if (relative != null) {
                under.put(relative, file.getValue());
            }
        }
        return under;
    }

    /**
     * The paths under {@code dir}, a directory relative to the lock's, relative to it; null when the host has not
     * said which paths exist.
     */
    public @Nullable Set<String> pathsUnder(String dir) {
        if (paths == null) {
            return null;
        }
        Set<String> under = new LinkedHashSet<>();
        for (String held : paths) {
            String relative = under(dir, held);
            if (relative != null) {
                under.add(relative);
            }
        }
        return under;
    }

    /**
     * The manifests under {@code dir}, a directory relative to the lock's, by directory relative to it.
     */
    public Map<String, String> manifestsUnder(String dir) {
        Map<String, String> under = new LinkedHashMap<>();
        for (Map.Entry<String, String> manifest : manifests.entrySet()) {
            String relative = manifest.getKey().equals(dir) ? "" : under(dir, manifest.getKey());
            if (relative != null) {
                under.put(relative, manifest.getValue());
            }
        }
        return under;
    }

    /**
     * The package.json of each directory above the lock's that the host holds, nearest first, keyed by the lock's
     * directory as seen from there ({@code packages/a}).
     */
    public Map<String, String> parentManifests() {
        Map<String, String> parents = new LinkedHashMap<>();
        String[] segments = path.isEmpty() ? new String[0] : path.split("/");
        StringBuilder up = new StringBuilder();
        for (int depth = 1; depth <= segments.length; depth++) {
            up.append("../");
            String manifest = files.get(up + "package.json");
            if (manifest != null) {
                StringBuilder below = new StringBuilder();
                for (int i = segments.length - depth; i < segments.length; i++) {
                    below.append(below.length() == 0 ? "" : "/").append(segments[i]);
                }
                parents.put(below.toString(), manifest);
            }
        }
        return parents;
    }

    /**
     * {@code dir}, a directory relative to the lock's, relative to the repository's root.
     */
    public String fromRoot(String dir) {
        return normalize(Paths.get(path.isEmpty() ? "." : path).resolve(dir.isEmpty() ? "." : dir));
    }

    private @Nullable String under(String dir, String relativePath) {
        String root = fromRoot(dir);
        String file = fromRoot(relativePath);
        if (root.isEmpty()) {
            return file.startsWith("../") ? null : file;
        }
        return file.startsWith(root + "/") ? file.substring(root.length() + 1) : null;
    }

    private static String normalize(Path file) {
        String normalized = file.normalize().toString().replace('\\', '/');
        return ".".equals(normalized) ? "" : normalized;
    }

    private static String directory(Path file) {
        Path dir = file.normalize().getParent();
        return dir == null ? "" : dir.toString().replace('\\', '/');
    }

    private static boolean contains(String dir, String other) {
        return dir.isEmpty() || other.equals(dir) || other.startsWith(dir + "/");
    }

    private static String relative(String root, String file) {
        String relative = Paths.get(root).relativize(Paths.get(file)).toString().replace('\\', '/');
        return ".".equals(relative) ? "" : relative;
    }

    private static @Nullable String projectNpmrc(@Nullable NodeResolutionResult marker) {
        if (marker == null || marker.getNpmrcConfigs() == null) {
            return null;
        }
        for (NodeResolutionResult.Npmrc npmrc : marker.getNpmrcConfigs()) {
            if (npmrc.getScope() == NodeResolutionResult.NpmrcScope.Project) {
                return Ini.encode(npmrc.getProperties());
            }
        }
        return null;
    }
}
