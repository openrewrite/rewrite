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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A non-optional peer that resolves to more than one version in the tree: {@code ajv-keywords} requires
 * {@code ajv@^8.8.2} while the tree holds both {@code ajv@6.15.0} (declared by the root) and {@code ajv@8.20.0}
 * (for {@code schema-utils} and {@code ajv-formats}). npm nests {@code ajv-keywords} under {@code schema-utils} beside
 * {@code ajv@8.20.0}, and {@code ajv-formats} sees the copy in its own {@code node_modules}. Each byte-exact test
 * replays the fixture OFFLINE and asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from
 * a real {@code npm install --package-lock-only} (npm 11.16.0).
 */
class NpmRequiredPeerForkLockRegenTest extends LockRegenTestSupport {

    private static final String FIXTURE = "lock/npm/peer-fork-required/";
    private static final String[] PACKUMENTS = {"@types/json-schema", "ajv", "ajv-formats", "ajv-keywords", "debug",
            "escape-string-regexp", "fast-deep-equal", "fast-json-stable-stringify", "fast-uri", "json-schema-traverse",
            "ms", "punycode", "require-from-string", "schema-utils", "uri-js"};
    private static final String[] MANIFESTS = {"@types/json-schema@7.0.15", "ajv-formats@3.0.1",
            "ajv-keywords@5.1.0", "ajv@6.15.0", "ajv@8.20.0", "debug@4.4.3", "escape-string-regexp@4.0.0",
            "fast-deep-equal@3.1.3", "fast-json-stable-stringify@2.1.0", "fast-uri@3.1.8", "json-schema-traverse@0.4.1",
            "json-schema-traverse@1.0.0", "ms@2.1.3", "punycode@2.3.1", "require-from-string@2.0.2",
            "schema-utils@4.5.0", "uri-js@4.4.1"};

    @BeforeEach
    void routes() {
        for (String name : PACKUMENTS) {
            routes.put(REG + requested(name), resource(FIXTURE + "http/" + file(name)));
        }
        for (String nameVersion : MANIFESTS) {
            int at = nameVersion.lastIndexOf('@');
            String name = nameVersion.substring(0, at);
            String version = nameVersion.substring(at + 1);
            routes.put(REG + requested(name) + "/" + version, resource(FIXTURE + "http/" + file(name) + "-" + version));
        }
    }

    @Test
    void requiredPeerForkPreservedThroughWholeClosureV3() {
        // An overrides edit forces whole-closure re-resolution.
        String before = resource(FIXTURE + "before");
        String original = resource(FIXTURE + "pkg-before");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, withOverride(original), original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(before);
    }

    @Test
    void reshapingSwapBesideRequiredPeerForkV3() {
        // debug keeps ms as its own dependency, a reshape only whole-closure re-resolution expresses.
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource(FIXTURE + "pkg-after-reshape"),
                resource(FIXTURE + "pkg-before"),
                resource(FIXTURE + "before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + "after-reshape"));
    }

    @Test
    void leafSwapBesideRequiredPeerForkV3() {
        // A leaf swap the per-dependency path patches directly, beside the peer fork.
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource(FIXTURE + "pkg-after"),
                resource(FIXTURE + "pkg-before"),
                resource(FIXTURE + "before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + "after"));
    }

    @Test
    void requiredPeerUnsatisfiedAtAPlacementDefers() {
        // Hand-edited: ajv-keywords at the top level sees ajv@6.15.0, outside its range; npm's re-placement is not
        // reproduced.
        String before = resource(FIXTURE + "before")
                .replace("\"node_modules/schema-utils/node_modules/ajv-keywords\"", "\"node_modules/ajv-keywords\"");
        String original = resource(FIXTURE + "pkg-before");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, withOverride(original), original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail())
                .contains("ajv-keywords@5.1.0 at node_modules/ajv-keywords")
                .contains("ajv@6.15.0");
    }

    @Test
    void peerFlaggedProviderDefers() {
        // Hand-edited: the ajv beside ajv-keywords is flagged as installed only for the peer, which the graph does
        // not model.
        String before = resource(FIXTURE + "before").replaceFirst(
                "(\"node_modules/schema-utils/node_modules/ajv\": \\{\n)", "$1      \"peer\": true,\n");
        assertThat(before).as("fixture edit applied").contains("\"peer\": true");
        String original = resource(FIXTURE + "pkg-before");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, withOverride(original), original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail())
                .contains("node_modules/schema-utils/node_modules/ajv")
                .contains("installed as a peer");
    }

    private static String requested(String name) {
        return name.replace("/", "%2F");
    }

    private static String file(String name) {
        return name.replace("/", "+");
    }

    /** An overrides entry naming no installed package: changes nothing, but forces whole-closure re-resolution. */
    private static String withOverride(String packageJson) {
        return packageJson.trim().replaceFirst("}$", ",\"overrides\":{\"z-nonexistent-xyz\":\"1.0.0\"}}");
    }
}
