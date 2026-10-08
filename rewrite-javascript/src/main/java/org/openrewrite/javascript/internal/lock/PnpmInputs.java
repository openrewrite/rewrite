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
 * A recipe's request as the pnpm regenerator takes it, by path from the workspace directory pnpm finds from the lock's
 * (the nearest holding a pnpm-workspace.yaml, else the lock's own); the marker's {@code .npmrc} when the host has none.
 */
final class PnpmInputs {

    private static final String WORKSPACE_MANIFEST = "pnpm-workspace.yaml";

    /**
     * For a walk that reads no registry document.
     */
    static final RegistryMetadata NO_REGISTRY = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return null;
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            return null;
        }
    };

    private PnpmInputs() {
    }

    static Inputs from(Request request, Host host, Instant now, RegistryMetadata registry) {
        String lockDir = NpmInputs.rootDirectory(request);
        String rootDir = workspaceDir(request, lockDir);
        Map<String, String> manifests = new LinkedHashMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : request.getPackageManagerFiles().entrySet()) {
            String path = underRoot(file.getKey(), rootDir);
            if (path == null) {
                continue;
            }
            if ("package.json".equals(path)) {
                manifests.put("", file.getValue());
            } else if (path.endsWith("/package.json")) {
                manifests.put(path.substring(0, path.length() - "/package.json".length()), file.getValue());
            } else {
                files.put(path, file.getValue());
            }
        }
        for (Map.Entry<String, String> manifest : NpmInputs.manifests(request, rootDir).entrySet()) {
            if (!manifest.getKey().equals("..") && !manifest.getKey().startsWith("../")) {
                manifests.put(manifest.getKey(), manifest.getValue());
            }
        }
        if (!files.containsKey(".npmrc") && rootDir.equals(lockDir)) {
            String npmrc = NpmInputs.projectNpmrc(request.getMarker());
            if (npmrc != null) {
                files.put(".npmrc", npmrc);
            }
        }
        return Inputs.builder()
                .lock(request.getLock())
                .lockDir(NpmInputs.location(rootDir, lockDir))
                .manifests(manifests)
                .files(files)
                .paths(paths(request, rootDir))
                .now(now)
                .registry(registry)
                .host(host)
                .build();
    }

    // findWorkspaceDir from the lock's directory up, over the package-manager files the host handed
    private static String workspaceDir(Request request, String lockDir) {
        String found = null;
        for (Path file : request.getPackageManagerFiles().keySet()) {
            Path name = file.getFileName();
            String dir = NpmInputs.directory(file);
            if (name != null && WORKSPACE_MANIFEST.equals(name.toString()) && NpmInputs.up(lockDir, dir) != null &&
                (found == null || dir.length() > found.length())) {
                found = dir;
            }
        }
        return found == null ? lockDir : found;
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
