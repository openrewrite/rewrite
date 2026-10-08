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
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A recipe's request as the Yarn Berry regenerator takes it: the manifests and paths under the lock's directory, and
 * the {@code .yarnrc.yml} of that directory and of each directory above it.
 */
final class YarnBerryInputs {

    private static final String RC_FILENAME = ".yarnrc.yml";

    private YarnBerryInputs() {
    }

    static Inputs from(Request request, Host host, Instant now) {
        String rootDir = NpmInputs.rootDirectory(request);
        Map<String, String> manifests = new LinkedHashMap<>();
        for (Map.Entry<String, String> manifest : NpmInputs.manifests(request, rootDir).entrySet()) {
            if (!manifest.getKey().equals("..") && !manifest.getKey().startsWith("../")) {
                manifests.put(manifest.getKey(), manifest.getValue());
            }
        }
        return Inputs.builder()
                .lock(request.getLock())
                .manifests(manifests)
                .configFiles(configFiles(request, rootDir))
                .paths(paths(request, rootDir))
                .host(host)
                .now(now)
                .build();
    }

    private static Map<String, String> configFiles(Request request, String rootDir) {
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            Path fileName = file.getKey().getFileName();
            String up = NpmInputs.up(rootDir, NpmInputs.directory(file.getKey()));
            if (fileName != null && RC_FILENAME.equals(fileName.toString()) && up != null) {
                files.put(up + RC_FILENAME, file.getValue());
            }
        }
        return files;
    }

    private static @Nullable Set<String> paths(Request request, String rootDir) {
        if (request.getProjectPaths() == null) {
            return null;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (Path file : request.getProjectPaths()) {
            String path = file.normalize().toString().replace('\\', '/');
            if (rootDir.isEmpty()) {
                paths.add(path);
            } else if (path.startsWith(rootDir + "/")) {
                paths.add(path.substring(rootDir.length() + 1));
            }
        }
        return paths;
    }
}
