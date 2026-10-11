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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.lock.bun.BunManifestRules;
import org.openrewrite.javascript.internal.lock.bun.DotEnv;
import org.openrewrite.javascript.internal.lock.bun.Lockfile;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Bun's port: the host's registries as the user's .npmrc, the lock's directory's own .npmrc and bunfig.toml, the patch
 * and .env files under it, and the package.json of each directory above the one bun runs in.
 */
final class BunInputs implements Port {

    static final BunInputs PORT = new BunInputs();

    private BunInputs() {
    }

    @Override
    public String lockFile() {
        return "bun.lock";
    }

    @Override
    public boolean reads(String fileName) {
        return fileName.endsWith(".patch") || readsOnlyBesideLock(fileName);
    }

    @Override
    public boolean readsOnlyBesideLock(String fileName) {
        return ".npmrc".equals(fileName) || "bunfig.toml".equals(fileName) || DotEnv.FILES.contains(fileName);
    }

    @Override
    public Result regenerate(LockDirectory dir, HostInputs host, NpmRegistryClient client) {
        return new BunLockRegenerator(from(dir, host), new BunRegistryManifests(host.transport(), client))
                .regenerate();
    }

    /**
     * With the host's registries as bun's user .npmrc, its user npmrc only: bun reads no global npmrc, and the
     * marker's project npmrc (the repository root's) only as the .npmrc beside the lock.
     */
    static Inputs from(LockDirectory dir, HostInputs host) {
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
                .userNpmrc(host.userNpmrc(HostInputs.USER))
                .files(files)
                .paths(dir.pathsUnder(""))
                .runsIn(dir.getRunsIn())
                .parentManifests(dir.manifestsAbove(dir.getRunsIn()))
                .host(host.getHost())
                .now(host.getNow())
                .build();
    }

    /**
     * bun's walk from where it runs; where it finds no bun.lock it stops at a file no regeneration writes (Lockfile's
     * rule).
     */
    @Override
    public ManifestRules manifestRules() {
        return BunManifestRules.RULES;
    }

    @Override
    public InstallRoot installRoot(LockDirectory dir) {
        InstallRoot walked = BunLockRegenerator.installRoot(dir.getRunsIn(), dir.getManifests(),
                dir.manifestsAbove(dir.getRunsIn()));
        String root = dir.aboveRunsIn(walked.getDepth());
        Predicate<String> holds = name -> dir.holds(RootRelativePath.join(root, name));
        if (walked.isStopped() || root.isEmpty() && dir.getLock() != null || holds.test("bun.lock")) {
            return walked;
        }
        return Lockfile.rewritesAnotherFile(holds) ?
                InstallRoot.stops(walked.getDepth(), InstallRoot.Stop.WRITES_ANOTHER_FILE) : walked;
    }
}
