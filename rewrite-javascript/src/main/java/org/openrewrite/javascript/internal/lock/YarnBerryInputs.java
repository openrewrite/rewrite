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

import org.openrewrite.javascript.internal.lock.berry.BerryOptions;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.openrewrite.javascript.internal.lock.berry.BerryOptions.DEFAULT_RC_FILENAME;

/**
 * The Yarn Berry regenerator's inputs: the manifests, paths and patch files under the lock's directory, and the
 * {@code .yarnrc.yml} of each directory from where Yarn runs up.
 */
final class YarnBerryInputs {

    private YarnBerryInputs() {
    }

    static boolean reads(String fileName) {
        return DEFAULT_RC_FILENAME.equals(fileName) || fileName.endsWith(".patch");
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> rcFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (file.getKey().equals(DEFAULT_RC_FILENAME) || file.getKey().endsWith("/" + DEFAULT_RC_FILENAME)) {
                rcFiles.put(file.getKey(), file.getValue());
            }
        }
        Map<String, String> patches = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.filesUnder("").entrySet()) {
            if (file.getKey().endsWith(".patch")) {
                patches.put(file.getKey(), file.getValue());
            }
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.manifestsUnder(""))
                .configFiles(BerryOptions.findRcFiles(dir.getRunsIn(), rcFiles))
                .runsIn(dir.getRunsIn())
                .files(patches)
                .paths(dir.pathsUnder(""))
                .host(host)
                .now(now)
                .build();
    }

    /**
     * Whether a file is there, by its path from the lock's directory ({@code ../yarn.lock}): read, held, the lock or a
     * manifest.
     */
    static Predicate<String> holds(LockDirectory dir) {
        return path -> dir.holds(path) || "yarn.lock".equals(path) && dir.getLock() != null ||
                       "package.json".equals(path) && dir.getManifests().containsKey("");
    }
}
