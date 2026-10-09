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

import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;

import java.time.Instant;
import java.util.Collections;

/**
 * The npm regenerator's inputs: the lock's directory's own .npmrc, and the package.json of each directory above it
 * when npm runs in that directory.
 */
final class NpmInputs {

    private NpmInputs() {
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return Inputs.builder()
                .lock(dir.getLock())
                .npmrc(dir.file(".npmrc"))
                .host(host)
                .now(now)
                .directoryName(dir.name())
                .manifests(dir.getManifests())
                .parentManifests(dir.getRunsIn().isEmpty() ? dir.parentManifests() : Collections.emptyMap())
                .build();
    }
}
