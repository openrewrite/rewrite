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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Port of @pnpm/workspace.root-finder src/index.ts (pnpm 11.2.2) over the files a project holds, by path.
 */
public final class WorkspaceRootFinder {

    static final String WORKSPACE_MANIFEST_FILENAME = "pnpm-workspace.yaml";

    private static final String WORKSPACE_DIR_ENV_VAR = "NPM_CONFIG_WORKSPACE_DIR";

    private static final List<String> WORKSPACE_MANIFEST_FILENAMES = Arrays.asList(WORKSPACE_MANIFEST_FILENAME,
            "pnpm-workspaces.yaml", "pnpm-workspaces.yml", "pnpm-workspace.yml", ".pnpm-workspace.yaml",
            ".pnpm-workspace.yml", ".pnpm-workspaces.yaml", ".pnpm-workspaces.yml");

    private WorkspaceRootFinder() {
    }

    /**
     * find-up over pnpm-workspace.yaml and the misspellings pnpm looks for, from cwd up: the path of the file
     * found, or null.
     */
    public static @Nullable String findWorkspaceManifest(Predicate<String> holds, String cwd) {
        for (String dir = cwd; ; dir = RootRelativePath.dirname(dir)) {
            for (String name : WORKSPACE_MANIFEST_FILENAMES) {
                String path = dir.isEmpty() ? name : dir + "/" + name;
                if (holds.test(path)) {
                    return path;
                }
            }
            if (dir.isEmpty()) {
                return null;
            }
        }
    }

    /**
     * {@code findWorkspaceDir}: the directory NPM_CONFIG_WORKSPACE_DIR names from cwd, whether or not it holds a
     * workspace manifest, else that of the one found from cwd, or null; an unknown environment (null) sets no variable.
     */
    static @Nullable String findWorkspaceDir(Predicate<String> holds, String cwd, @Nullable Map<String, String> env) {
        String workspaceManifestDirEnvVar = env == null ? null : env.containsKey(WORKSPACE_DIR_ENV_VAR) ?
                env.get(WORKSPACE_DIR_ENV_VAR) : env.get(WORKSPACE_DIR_ENV_VAR.toLowerCase(Locale.ROOT));
        String workspaceManifestLocation;
        if (workspaceManifestDirEnvVar != null && !workspaceManifestDirEnvVar.isEmpty()) {
            String dir = RootRelativePath.join(cwd, workspaceManifestDirEnvVar);
            if (workspaceManifestDirEnvVar.startsWith("/") || workspaceManifestDirEnvVar.matches("^[A-Za-z]:.*") ||
                RootRelativePath.climbsAboveRoot(dir)) {
                throw new CannotKnow("the workspace " + WORKSPACE_DIR_ENV_VAR + " names (" +
                                     workspaceManifestDirEnvVar + ", outside the project)");
            }
            workspaceManifestLocation = RootRelativePath.join(dir, WORKSPACE_MANIFEST_FILENAME);
        } else {
            workspaceManifestLocation = findWorkspaceManifest(holds, cwd);
        }
        if (workspaceManifestLocation == null) {
            return null;
        }
        if (!workspaceManifestLocation.equals(WORKSPACE_MANIFEST_FILENAME) &&
            !workspaceManifestLocation.endsWith("/" + WORKSPACE_MANIFEST_FILENAME)) {
            throw new PnpmError("BAD_WORKSPACE_MANIFEST_NAME", "The workspace manifest file should be named " +
                    "\"pnpm-workspace.yaml\". File found: " + workspaceManifestLocation);
        }
        return RootRelativePath.dirname(workspaceManifestLocation);
    }
}
