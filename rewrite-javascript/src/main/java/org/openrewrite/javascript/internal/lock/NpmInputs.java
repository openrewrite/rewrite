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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.lock.npm.Ini;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A recipe's request as the npm regenerator takes it: the manifests by path, placed relative to the lock's
 * directory, which is that of the topmost manifest given, else the edited manifest's own. At the repository
 * root the directory's name is not known.
 */
final class NpmInputs {

    private NpmInputs() {
    }

    static Inputs from(Request request, Host host, Instant now) {
        String rootDir = rootDirectory(request);
        Path rootName = Paths.get(rootDir).getFileName();
        return Inputs.builder()
                .lock(request.getLock())
                .npmrc(projectNpmrc(request.getMarker()))
                .host(host)
                .now(now)
                .directoryName(rootDir.isEmpty() || rootName == null ? null : rootName.toString())
                .manifests(manifests(request, rootDir))
                .build();
    }

    /**
     * The lock's directory: that of the topmost manifest given that contains the edited one, else the edited
     * manifest's own.
     */
    static String rootDirectory(Request request) {
        String dir = directory(packageJsonPath(request));
        String rootDir = dir;
        for (Path path : request.getManifests().keySet()) {
            String candidate = directory(path);
            if (contains(candidate, dir) && candidate.length() < rootDir.length()) {
                rootDir = candidate;
            }
        }
        return rootDir;
    }

    /**
     * The manifests by location relative to the lock's directory, the edited one last.
     */
    static Map<String, String> manifests(Request request, String rootDir) {
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<Path, String> manifest : request.getManifests().entrySet()) {
            manifests.put(location(rootDir, directory(manifest.getKey())), manifest.getValue());
        }
        manifests.put(location(rootDir, directory(packageJsonPath(request))), request.getPackageJson());
        return manifests;
    }

    private static Path packageJsonPath(Request request) {
        return request.getPackageJsonPath() == null ? Paths.get("package.json") : request.getPackageJsonPath();
    }

    static String directory(Path file) {
        Path dir = file.normalize().getParent();
        return dir == null ? "" : dir.toString().replace('\\', '/');
    }

    private static boolean contains(String dir, String other) {
        return dir.isEmpty() || other.equals(dir) || other.startsWith(dir + "/");
    }

    private static String location(String rootDir, String dir) {
        String location = Paths.get(rootDir).relativize(Paths.get(dir)).toString().replace('\\', '/');
        return ".".equals(location) ? "" : location;
    }

    // only the project's own npmrc is an input
    static @Nullable String projectNpmrc(@Nullable NodeResolutionResult marker) {
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
