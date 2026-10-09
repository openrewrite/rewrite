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

import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator.Inputs;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Bun regenerator's inputs: the lock's directory's own .npmrc and bunfig.toml, the patch and .env files under it,
 * and the package.json of each directory above it when bun runs in that directory.
 */
final class BunInputs {

    private static final List<String> DOT_ENV = Arrays.asList(".env.production.local", ".env.local",
            ".env.production", ".env");

    private BunInputs() {
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.filesUnder("").entrySet()) {
            if (file.getKey().endsWith(".patch") || DOT_ENV.contains(file.getKey())) {
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
                .parentManifests(dir.getRunsIn().isEmpty() ? dir.parentManifests() : Collections.emptyMap())
                .host(host)
                .now(now)
                .build();
    }
}
