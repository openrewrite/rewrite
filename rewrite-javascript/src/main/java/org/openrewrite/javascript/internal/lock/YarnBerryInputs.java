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

/**
 * The Yarn Berry regenerator's inputs: the manifests, paths and patch files under the lock's directory, and the
 * {@code .yarnrc.yml} of that directory and of each directory above it.
 */
final class YarnBerryInputs {

    private YarnBerryInputs() {
    }

    static boolean reads(String fileName) {
        return ".yarnrc.yml".equals(fileName) || fileName.endsWith(".patch");
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> configFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            if (file.getKey().matches("(\\.\\./)*" + "\\.yarnrc\\.yml")) {
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
                .files(patches)
                .paths(dir.pathsUnder(""))
                .host(host)
                .now(now)
                .build();
    }
}
