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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Removing a root dependency that a remaining package still lists as a peer. Goldens are recorded from a real
 * {@code npm install --package-lock-only} (npm 11.16.0) and replayed OFFLINE.
 */
class NpmPeerRemovalLockRegenTest extends LockRegenTestSupport {

    @Test
    void removingAnOptionalPeerRemovesIt() {
        // npm does not keep a package only an optional peer (fdir -> picomatch) reaches.
        String fixture = "lock/npm/optional-peer-removal/";
        route(fixture, "fdir@6.5.0", "picomatch@4.0.7");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, resource(fixture + "pkg-after"),
                resource(fixture + "pkg-before"), resource(fixture + "before"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(fixture + "after"));
    }

    @Test
    void removingARequiredPeerDefers() {
        // npm keeps ajv for ajv-keywords' required peer and flags it and its peer-only subtree peer: true
        // (the recorded after); auto-installing a non-leaf peer is not reproduced, so it defers.
        String fixture = "lock/npm/required-peer-removal/";
        route(fixture, "ajv@8.20.0", "ajv-keywords@5.1.0", "fast-deep-equal@3.1.3", "fast-uri@3.1.8",
                "json-schema-traverse@1.0.0", "require-from-string@2.0.2");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, resource(fixture + "pkg-after"),
                resource(fixture + "pkg-before"), resource(fixture + "before"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("ajv-keywords peer ajv is not installed");
    }

    private void route(String fixture, String... nameVersions) {
        for (String nameVersion : nameVersions) {
            int at = nameVersion.lastIndexOf('@');
            String name = nameVersion.substring(0, at);
            routes.put(REG + name, resource(fixture + "http/" + name));
            routes.put(REG + name + "/" + nameVersion.substring(at + 1), resource(fixture + "http/" + name + "-" +
                    nameVersion.substring(at + 1)));
        }
    }
}
