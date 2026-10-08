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
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.pnpm.RegistryMetadata;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A recipe's request as the pnpm regenerator takes it: the manifests and package-manager files under the lock's
 * directory by path from it, and the paths the project holds there; the project's {@code .npmrc} from the marker when
 * the host did not hand the file.
 */
final class PnpmInputs {

    private PnpmInputs() {
    }

    static Inputs from(Request request, Host host, Instant now, RegistryMetadata registry) {
        String rootDir = NpmInputs.rootDirectory(request);
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<String, String> manifest : NpmInputs.manifests(request, rootDir).entrySet()) {
            if (!manifest.getKey().equals("..") && !manifest.getKey().startsWith("../")) {
                manifests.put(manifest.getKey(), manifest.getValue());
            }
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            String path = underRoot(file.getKey(), rootDir);
            if (path != null) {
                files.put(path, file.getValue());
            }
        }
        if (!files.containsKey(".npmrc")) {
            String npmrc = NpmInputs.projectNpmrc(request.getMarker());
            if (npmrc != null) {
                files.put(".npmrc", npmrc);
            }
        }
        return Inputs.builder()
                .lock(request.getLock())
                .manifests(manifests)
                .files(files)
                .paths(paths(request, rootDir))
                .now(now)
                .registry(registry)
                .host(host)
                .build();
    }

    private static @Nullable Set<String> paths(Request request, String rootDir) {
        if (request.getProjectPaths() == null) {
            return null;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (Path file : request.getProjectPaths()) {
            String path = underRoot(file, rootDir);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static @Nullable String underRoot(Path file, String rootDir) {
        String path = file.normalize().toString().replace('\\', '/');
        if (rootDir.isEmpty()) {
            return path;
        }
        return path.startsWith(rootDir + "/") ? path.substring(rootDir.length() + 1) : null;
    }
}
