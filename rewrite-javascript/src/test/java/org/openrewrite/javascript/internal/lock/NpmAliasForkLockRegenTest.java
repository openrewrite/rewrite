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
 * The npm alias-fork case through the whole {@link NativeLockEngine}: one or more {@code npm:} aliases of a real
 * package (the {@code @isaacs/cliui} {@code string-width} / {@code string-width-cjs} shape), with or without an
 * un-aliased copy. Every alias lands at its own top-level slot carrying a {@code name} field for the real package
 * and the real package's {@code resolved}/{@code integrity}. Each byte-exact test replays a fixture entirely
 * OFFLINE (a stub {@code HttpSender} serves the real package's packument/manifests) and asserts the emitted lock
 * is BYTE-IDENTICAL to a golden {@code after} recorded from a real
 * {@code npm install --package-lock-only --lockfile-version 3} (npm 10.9.2).
 */
class NpmAliasForkLockRegenTest extends LockRegenTestSupport {

    @Test
    void aliasForkAddV3() {
        // The lock already resolves emoji-regex@10.6.0 (the un-aliased copy). Adding an emoji-regex-cjs alias of
        // emoji-regex@^8 forks the same real package: npm inserts a second top-level entry keyed emoji-regex-cjs
        // with a `name` field of emoji-regex, leaving the un-aliased entry byte-identical.
        routes.put(REG + "emoji-regex", resource("lock/npm/alias-fork/http/emoji-regex"));
        routes.put(REG + "emoji-regex/8.0.0", resource("lock/npm/alias-fork/http/emoji-regex-8.0.0"));
        routes.put(REG + "emoji-regex/10.6.0", resource("lock/npm/alias-fork/http/emoji-regex-10.6.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/alias-fork/pkg-after"),
                resource("lock/npm/alias-fork/pkg-before"),
                resource("lock/npm/alias-fork/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/alias-fork/after"));
    }

    @Test
    void multiAliasForkAddSameVersionV3() {
        // er-x already aliases emoji-regex@8; adding a second alias er-y of the same package places an
        // independent top-level entry and leaves er-x byte-identical.
        routes.put(REG + "emoji-regex", resource("lock/npm/multi-alias-same/http/emoji-regex"));
        routes.put(REG + "emoji-regex/8.0.0", resource("lock/npm/multi-alias-same/http/emoji-regex-8.0.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/multi-alias-same/pkg-after"),
                resource("lock/npm/multi-alias-same/pkg-before"),
                resource("lock/npm/multi-alias-same/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/multi-alias-same/after"));
    }

    @Test
    void multiAliasForkAddDifferentVersionsV3() {
        // er8 aliases emoji-regex@8; adding er10 aliasing emoji-regex@10 places a second alias entry at its own
        // version, sorted before er8 (er10 < er8) in both the packages map and the importer edges.
        routes.put(REG + "emoji-regex", resource("lock/npm/multi-alias-diff/http/emoji-regex"));
        routes.put(REG + "emoji-regex/8.0.0", resource("lock/npm/multi-alias-diff/http/emoji-regex-8.0.0"));
        routes.put(REG + "emoji-regex/10.6.0", resource("lock/npm/multi-alias-diff/http/emoji-regex-10.6.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/multi-alias-diff/pkg-after"),
                resource("lock/npm/multi-alias-diff/pkg-before"),
                resource("lock/npm/multi-alias-diff/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/npm/multi-alias-diff/after"));
    }

    @Test
    void duplicateNestedPlacementPreservedV3() {
        // stripa and stripb both alias strip-ansi@6, which needs ansi-regex@5 while the top-level slot holds
        // ansi-regex@6, so npm nests a private ansi-regex@5 under each. An overrides edit forces whole-closure
        // re-resolution, which must preserve both copies of the duplicated ansi-regex@5 byte-identically.
        routes.put(REG + "strip-ansi/6.0.1", resource("lock/npm/dup-placement/http/strip-ansi-6.0.1"));
        routes.put(REG + "ansi-regex/6.3.0", resource("lock/npm/dup-placement/http/ansi-regex-6.3.0"));
        routes.put(REG + "ansi-regex/5.0.1", resource("lock/npm/dup-placement/http/ansi-regex-5.0.1"));

        String before = resource("lock/npm/dup-placement/before");
        String original = resource("lock/npm/dup-placement/pkg-before");
        String edited = original.trim().replaceFirst("}$", ",\"overrides\":{\"z-nonexistent-xyz\":\"1.0.0\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm, edited, original, before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(before);
    }

    @Test
    void aliasAddIntoV2LockDefers() {
        // The alias resolves through whole-closure, but a fresh alias entry in a lockfileVersion 2 legacy tree is
        // not yet serialized, so it fails loud rather than emit an unverified layout.
        routes.put(REG + "emoji-regex", resource("lock/npm/alias-fork/http/emoji-regex"));
        routes.put(REG + "emoji-regex/8.0.0", resource("lock/npm/alias-fork/http/emoji-regex-8.0.0"));
        routes.put(REG + "emoji-regex/10.6.0", resource("lock/npm/alias-fork/http/emoji-regex-10.6.0"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/alias-fork/pkg-after"),
                resource("lock/npm/alias-fork/pkg-before"),
                resource("lock/npm/alias-fork/before-v2"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("lockfileVersion 2");
    }
}
