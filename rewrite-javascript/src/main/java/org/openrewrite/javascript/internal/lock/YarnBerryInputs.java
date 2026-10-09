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

import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The Yarn Berry regenerator's inputs: the manifests, paths and patch files under the lock's directory, and the
 * {@code .yarnrc.yml} of each directory from where Yarn runs up.
 */
final class YarnBerryInputs {

    private static final String RC_FILENAME = ".yarnrc.yml";

    private YarnBerryInputs() {
    }

    static boolean reads(String fileName) {
        return RC_FILENAME.equals(fileName) || fileName.endsWith(".patch");
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> configFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (file.getKey().matches("(\\.\\./)*" + "\\.yarnrc\\.yml") || onTheWayTo(file.getKey(), dir.getRunsIn())) {
                configFiles.put(file.getKey(), file.getValue());
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
                .configFiles(configFiles)
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
        return path -> dir.getFiles().containsKey(path) || dir.getPaths() != null && dir.getPaths().contains(path) ||
                       "yarn.lock".equals(path) && dir.getLock() != null ||
                       "package.json".equals(path) && dir.getManifests().containsKey("");
    }

    // an rc file in a directory between the lock's and the one Yarn runs in, or in that one
    private static boolean onTheWayTo(String path, String runsIn) {
        if (!path.endsWith("/" + RC_FILENAME) || path.startsWith("../")) {
            return false;
        }
        String parent = path.substring(0, path.length() - RC_FILENAME.length() - 1);
        return runsIn.equals(parent) || runsIn.startsWith(parent + "/");
    }
}
