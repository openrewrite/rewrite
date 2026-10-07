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
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A recipe's request as the Yarn classic regenerator takes it, with the config files of the lock's directory and of
 * each directory above it, and those directories' package.json; the marker's npmrc stands in for a missing .npmrc.
 */
final class YarnClassicInputs {

    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");

    private YarnClassicInputs() {
    }

    static Inputs from(Request request, Host host, Instant now) {
        String rootDir = NpmInputs.rootDirectory(request);
        Map<String, String> configFiles = configFiles(request, rootDir);
        if (!configFiles.containsKey(".npmrc")) {
            String npmrc = NpmInputs.projectNpmrc(request.getMarker());
            if (npmrc != null) {
                configFiles.put(".npmrc", npmrc);
            }
        }
        return Inputs.builder()
                .lock(request.getLock())
                .manifests(NpmInputs.manifests(request, rootDir))
                .configFiles(configFiles)
                .paths(paths(request, rootDir))
                .directory(rootDir)
                .host(host)
                .now(now)
                .build();
    }

    private static Map<String, String> configFiles(Request request, String rootDir) {
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            Path fileName = file.getKey().getFileName();
            String up = fileName == null ? null : up(rootDir, NpmInputs.directory(file.getKey()));
            if (up != null && (CONFIG_FILES.contains(fileName.toString()) ||
                               !up.isEmpty() && "package.json".equals(fileName.toString()))) {
                files.put(up + fileName, file.getValue());
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
            String up = file.getFileName() == null ? null : up(rootDir, NpmInputs.directory(file));
            if (rootDir.isEmpty()) {
                paths.add(path);
            } else if (path.startsWith(rootDir + "/")) {
                paths.add(path.substring(rootDir.length() + 1));
            } else if (up != null) {
                paths.add(up + file.getFileName());
            }
        }
        return paths;
    }

    // "../" for each directory from the root's up to dir, when dir is the root's or one above it
    private static @Nullable String up(String rootDir, String dir) {
        if (!dir.isEmpty() && !dir.equals(rootDir) && !rootDir.startsWith(dir + "/")) {
            return null;
        }
        int depth = segments(rootDir) - segments(dir);
        StringBuilder up = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            up.append("../");
        }
        return up.toString();
    }

    private static int segments(String dir) {
        return dir.isEmpty() ? 0 : dir.split("/").length;
    }
}
