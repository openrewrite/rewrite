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

import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceRootFinder;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The pnpm regenerator's inputs, by path from the workspace directory pnpm finds from the lock's: the manifests, and
 * the files pnpm reads there (pnpm-workspace.yaml, .npmrc, pnpmfiles, patch files).
 */
final class PnpmInputs {

    private static final String WORKSPACE_MANIFEST = "pnpm-workspace.yaml";
    private static final String LOCK = "pnpm-lock.yaml";

    private PnpmInputs() {
    }

    // a pnpmfile only by its path: pnpm runs it, which the engine cannot
    static boolean reads(String fileName) {
        return WORKSPACE_MANIFEST.equals(fileName) || ".npmrc".equals(fileName) || fileName.endsWith(".patch");
    }

    static boolean readsAsManifest(String fileName) {
        return PnpmLockRegenerator.OTHER_MANIFEST_NAMES.contains(fileName);
    }

    // a workspace whose projects keep their own locks: pnpm installs every project in the same run
    static boolean readsOtherLocks() {
        return true;
    }

    static InstallRoot installRoot(LockDirectory dir) {
        return PnpmLockRegenerator.installRoot(project(dir).build());
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return project(dir).host(host).now(now).build();
    }

    // what pnpm reads of the project, which is all its walk reads
    private static Inputs.InputsBuilder project(LockDirectory dir) {
        String workspaceDir = workspaceDir(dir);
        Map<String, String> manifests = new LinkedHashMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.filesUnder(workspaceDir).entrySet()) {
            String path = file.getKey();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if ("package.json".equals(path)) {
                manifests.put("", file.getValue());
            } else if (path.endsWith("/package.json")) {
                manifests.put(path.substring(0, path.length() - "/package.json".length()), file.getValue());
            } else if (reads(name) || LOCK.equals(name) || readsAsManifest(name)) {
                files.put(path, file.getValue());
            }
        }
        manifests.putAll(dir.manifestsUnder(workspaceDir));
        String lockDir = relative(dir.fromRoot(workspaceDir), dir.getPath());
        return Inputs.builder()
                .lock(dir.getLock())
                .lockDir(lockDir)
                .dir(relative(dir.fromRoot(workspaceDir), dir.fromRoot(dir.getRunsIn())))
                .manifests(manifests)
                .files(files)
                .paths(dir.pathsUnder(workspaceDir));
    }

    // the nearest directory from the lock's up holding a pnpm-workspace.yaml, read or not, else a misspelt one pnpm
    // stops at; one misspelt below the root is under it, where the regenerator's findWorkspaceDir sees it
    private static String workspaceDir(LockDirectory dir) {
        Set<String> held = new LinkedHashSet<>();
        for (String file : dir.getFiles().keySet()) {
            held.add(dir.fromRoot(file));
        }
        if (dir.getPaths() != null) {
            for (String path : dir.getPaths()) {
                held.add(dir.fromRoot(path));
            }
        }
        String found = WorkspaceRootFinder.findWorkspaceManifest(path -> held.contains(path) &&
                ("/" + path).endsWith("/" + WORKSPACE_MANIFEST), dir.getPath());
        if (found == null) {
            found = WorkspaceRootFinder.findWorkspaceManifest(held::contains, dir.getPath());
        }
        if (found == null) {
            return "";
        }
        int slash = found.lastIndexOf('/');
        return relative(dir.getPath(), slash < 0 ? "" : found.substring(0, slash));
    }

    private static String relative(String from, String to) {
        String relative = Paths.get(from).relativize(Paths.get(to)).toString().replace('\\', '/');
        return ".".equals(relative) ? "" : relative;
    }
}
