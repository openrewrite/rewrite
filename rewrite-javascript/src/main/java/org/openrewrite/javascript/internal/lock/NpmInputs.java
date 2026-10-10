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
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;

import java.time.Instant;

/**
 * The npm regenerator's inputs: the lock's directory's own .npmrc and the paths held there, and the package.json of
 * each directory above the one npm runs in.
 */
final class NpmInputs {

    private NpmInputs() {
    }

    static boolean readsOnlyBesideLock(String fileName) {
        return ".npmrc".equals(fileName);
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return Inputs.builder()
                .lock(dir.getLock())
                .npmrc(dir.file(".npmrc"))
                .paths(dir.pathsUnder(""))
                .host(host)
                .now(now)
                .directoryName(dir.name())
                .manifests(dir.getManifests())
                .runsIn(dir.getRunsIn())
                .parentManifests(dir.manifestsAbove(dir.getRunsIn()))
                .build();
    }

    /**
     * npm's walk from where it runs; where it installs it reads and rewrites a yarn.lock (shrinkwrap.js save), which
     * no regeneration writes, so a walk to one the host holds stops.
     */
    static InstallRoot installRoot(LockDirectory dir) {
        InstallRoot walked = NpmLockRegenerator.installRoot(from(dir, Host.UNKNOWN, Instant.EPOCH));
        String root = dir.aboveRunsIn(walked.getDepth());
        return walked.isStopped() || !dir.holds(RootRelativePath.join(root, "yarn.lock")) ? walked :
                new InstallRoot(walked.getDepth(), true);
    }
}
