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
 * A peer dependency that resolves to more than one version in the tree (a "peer fork"): {@code fdir} declares an
 * optional peer {@code picomatch@^3 || ^4} while the tree holds both {@code picomatch@2.3.2} (for
 * {@code micromatch}) and {@code picomatch@4.0.7} (for {@code tinyglobby}). npm satisfies the peer per placement,
 * nesting {@code fdir} under {@code tinyglobby} beside {@code picomatch@4.0.7} while {@code picomatch@2.3.2} holds
 * the top level. Each byte-exact test replays the fixture OFFLINE and asserts the emitted lock is BYTE-IDENTICAL to
 * a golden {@code after} recorded from a real {@code npm install --package-lock-only} (npm 11.19.0).
 */
class NpmPeerForkLockRegenTest extends LockRegenTestSupport {

    private static final String[] PACKUMENTS = {"braces", "escape-string-regexp", "fdir", "fill-range",
            "is-number", "micromatch", "ms", "picomatch", "tinyglobby", "to-regex-range"};
    private static final String[] MANIFESTS = {"braces@3.0.3", "escape-string-regexp@4.0.0", "fdir@6.5.0",
            "fill-range@7.1.1", "is-number@7.0.0", "micromatch@4.0.8", "ms@2.1.3", "picomatch@2.3.2",
            "picomatch@4.0.7", "tinyglobby@0.2.14", "to-regex-range@5.0.1"};

    @BeforeEach
    void routes() {
        for (String name : PACKUMENTS) {
            routes.put(REG + name, resource("lock/npm/peer-fork/http/" + name));
        }
        for (String nameVersion : MANIFESTS) {
            int at = nameVersion.lastIndexOf('@');
            String name = nameVersion.substring(0, at);
            String version = nameVersion.substring(at + 1);
            routes.put(REG + name + "/" + version, resource("lock/npm/peer-fork/http/" + name + "-" + version));
        }
    }

    @Test
    void peerForkPreservedThroughWholeClosureV3() {
        // An overrides edit forces whole-closure re-resolution. Every placement of fdir sees a picomatch its peer
        // range admits, so the peer fork is kept exactly as npm laid it out.
        String before = resource("lock/npm/peer-fork/before");
        String original = resource("lock/npm/peer-fork/pkg-before");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, withOverride(original), original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(before);
    }

    @Test
    void swapBesidePeerForkThroughWholeClosureV3() {
        // Swapping ms for escape-string-regexp while re-resolving the whole closure (the path a package swap takes
        // when its closure cannot be patched directly): only the swapped entries change, matching npm.
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                withOverride(resource("lock/npm/peer-fork/pkg-after")),
                resource("lock/npm/peer-fork/pkg-before"),
                resource("lock/npm/peer-fork/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/peer-fork/after"));
    }

    @Test
    void swapBesidePeerForkV3() {
        // The same swap without forcing whole-closure: removing ms from a lock that has nested placements elsewhere
        // is garbage-collected by node_modules resolution, so it no longer needs a flat tree.
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/peer-fork/pkg-after"),
                resource("lock/npm/peer-fork/pkg-before"),
                resource("lock/npm/peer-fork/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/peer-fork/after"));
    }

    @Test
    void removalOrphaningADuplicatedNameDefers() {
        // Removing micromatch orphans its closure, including the top-level picomatch@2.3.2 while picomatch@4.0.7
        // stays nested under tinyglobby; npm could hoist that copy into the freed slot, which is not reproduced, so
        // it defers rather than emit a lock that may not match.
        String original = resource("lock/npm/peer-fork/pkg-before");
        String edited = original.replace("    \"micromatch\": \"4.0.8\",\n", "");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, edited, original,
                resource("lock/npm/peer-fork/before"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
    }

    @Test
    void peerUnsatisfiedAtAPlacementDefers() {
        // A (hand-edited) layout where fdir sits at the top level, where the picomatch it sees is 2.3.2, outside
        // its peer range. npm would re-place fdir to satisfy the peer; that move is not reproduced, so it defers.
        String before = resource("lock/npm/peer-fork/before")
                .replace("\"node_modules/tinyglobby/node_modules/fdir\"", "\"node_modules/fdir\"");
        String original = resource("lock/npm/peer-fork/pkg-before");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, withOverride(original), original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail())
                .contains("fdir@6.5.0 at node_modules/fdir")
                .contains("picomatch@2.3.2");
    }

    /** An overrides entry naming no installed package: changes nothing, but forces whole-closure re-resolution. */
    private static String withOverride(String packageJson) {
        return packageJson.trim().replaceFirst("}$", ",\"overrides\":{\"z-nonexistent-xyz\":\"1.0.0\"}}");
    }
}
