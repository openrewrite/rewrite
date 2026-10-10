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

import java.util.ArrayList;
import java.util.List;

/**
 * Port of @pnpm/hooks.pnpmfile src/requireHooks.ts (pnpm 11.2.2): every pnpmfile it would load is JavaScript pnpm
 * runs, which the engine cannot; one it cannot find is pnpm's error.
 */
final class RequireHooks {

    private final PnpmLockRegenerator.Inputs inputs;

    RequireHooks(PnpmLockRegenerator.Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * {@code requireHooks(prefix, opts)}: the global pnpmfile, the default .pnpmfile.mjs or .pnpmfile.cjs, then each
     * pnpmfile listed, from {@code prefix}.
     */
    void requireHooks(String prefix, @Nullable Object globalPnpmfile, List<?> pnpmfiles,
                      boolean tryLoadDefaultPnpmfile) {
        if (Js.truthy(globalPnpmfile)) {
            throw new CannotKnow("what " + Js.string(globalPnpmfile) + " does (pnpm runs its hooks)");
        }
        List<String> loadedFiles = new ArrayList<>();
        if (tryLoadDefaultPnpmfile) {
            for (String defaultPnpmfile : new String[]{".pnpmfile.mjs", ".pnpmfile.cjs"}) {
                String path = RootRelativePath.join(prefix, defaultPnpmfile);
                if (inputs.holds(path)) {
                    throw new CannotKnow("what " + path + " does (pnpm runs its hooks)");
                }
            }
            loadedFiles.add(RootRelativePath.join(prefix, ".pnpmfile.cjs"));
        }
        String missing = null;
        for (Object pnpmfile : pnpmfiles) {
            if (!(pnpmfile instanceof String)) {
                throw new CannotKnow("what pnpm makes of a pnpmfile that is a " + Js.typeOf(pnpmfile) + " (where " +
                                     "pnpm reads a path)");
            }
            String path = (String) pnpmfile;
            String file = RootRelativePath.join(prefix, path);
            if (path.startsWith("/") || path.startsWith("~") || path.matches("^[A-Za-z]:.*") ||
                RootRelativePath.climbsAboveRoot(file)) {
                throw new CannotKnow("whether the pnpmfile " + path + " exists (a path outside the project)");
            }
            if (loadedFiles.contains(file)) {
                continue;
            }
            loadedFiles.add(file);
            if (inputs.holds(file)) {
                throw new CannotKnow("what " + file + " does (pnpm runs its hooks)");
            }
            if (inputs.getPaths() == null) {
                throw new CannotKnow("whether the pnpmfile " + file + " exists (the host does not say which files " +
                                     "the project holds)");
            }
            if (missing == null) {
                missing = file;
            }
        }
        if (missing != null) {
            throw new PnpmError("PNPMFILE_NOT_FOUND", "pnpmfile at \"" + missing + "\" is not found");
        }
    }
}
