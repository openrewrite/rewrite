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
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A recipe's request as the Bun regenerator takes it: the manifests placed as {@link NpmInputs} places them, the
 * .npmrc and bunfig.toml of the lock's directory, and the paths of the package-manager files under it (the patch
 * files). Without the .npmrc file itself, the project's npmrc the marker read stands in for it.
 */
final class BunInputs {

    private BunInputs() {
    }

    static Inputs from(Request request, Host host, Instant now) {
        String rootDir = NpmInputs.rootDirectory(request);
        String npmrc = file(request, rootDir, ".npmrc");
        return Inputs.builder()
                .lock(request.getLock())
                .manifests(NpmInputs.manifests(request, rootDir))
                .npmrc(npmrc != null ? npmrc : NpmInputs.projectNpmrc(request.getMarker()))
                .bunfig(file(request, rootDir, "bunfig.toml"))
                .files(files(request, rootDir))
                .host(host)
                .now(now)
                .build();
    }

    // every package-manager file under the root, by path relative to it
    private static Set<String> files(Request request, String rootDir) {
        Set<String> files = new LinkedHashSet<>();
        for (Path file : request.getPackageManagerFiles().keySet()) {
            String path = file.normalize().toString().replace('\\', '/');
            if (rootDir.isEmpty()) {
                files.add(path);
            } else if (path.startsWith(rootDir + "/")) {
                files.add(path.substring(rootDir.length() + 1));
            }
        }
        return files;
    }

    private static @Nullable String file(Request request, String rootDir, String name) {
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            Path fileName = file.getKey().getFileName();
            if (fileName != null && name.equals(fileName.toString()) &&
                rootDir.equals(NpmInputs.directory(file.getKey()))) {
                return file.getValue();
            }
        }
        return null;
    }
}
