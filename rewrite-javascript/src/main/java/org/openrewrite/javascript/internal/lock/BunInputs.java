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
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.bun.DotEnv;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Bun regenerator's inputs: the lock's directory's own .npmrc and bunfig.toml, the patch and .env files under it,
 * and the package.json of each directory above the one bun runs in.
 */
final class BunInputs {

    private BunInputs() {
    }

    static boolean reads(String fileName) {
        return fileName.endsWith(".patch") || readsOnlyBesideLock(fileName);
    }

    static boolean readsOnlyBesideLock(String fileName) {
        return ".npmrc".equals(fileName) || "bunfig.toml".equals(fileName) || DotEnv.FILES.contains(fileName);
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.filesUnder("").entrySet()) {
            if (DotEnv.FILES.contains(file.getKey())) {
                files.put(file.getKey(), file.getValue());
            }
        }
        // a patch file is wherever patchedDependencies says, above the lock's directory too
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (file.getKey().endsWith(".patch")) {
                files.put(file.getKey(), file.getValue());
            }
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.getManifests())
                .npmrc(dir.file(".npmrc"))
                .bunfig(dir.file("bunfig.toml"))
                .files(files)
                .paths(dir.pathsUnder(""))
                .runsIn(dir.getRunsIn())
                .parentManifests(dir.manifestsAbove(dir.getRunsIn()))
                .host(host)
                .now(now)
                .build();
    }

    /**
     * bun's walk from where it runs; it stops where bun, finding no bun.lock, rewrites a bun.lockb, or migrates a
     * pnpm-lock.yaml (no package-lock.json or yarn.lock first) and rewrites package.json, which no regeneration writes.
     */
    static InstallRoot installRoot(LockDirectory dir) {
        InstallRoot walked = BunLockRegenerator.installRoot(from(dir, Host.UNKNOWN, Instant.EPOCH));
        String root = dir.aboveRunsIn(walked.getDepth());
        if (walked.isStopped() || root.isEmpty() && dir.getLock() != null || holds(dir, root, "bun.lock")) {
            return walked;
        }
        boolean rewritten = holds(dir, root, "bun.lockb") || !holds(dir, root, "package-lock.json") &&
                            !holds(dir, root, "yarn.lock") && holds(dir, root, "pnpm-lock.yaml");
        return rewritten ? new InstallRoot(walked.getDepth(), true) : walked;
    }

    private static boolean holds(LockDirectory dir, String root, String file) {
        return dir.holds(RootRelativePath.join(root, file));
    }
}
