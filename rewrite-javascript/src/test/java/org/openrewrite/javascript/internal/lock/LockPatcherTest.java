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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Byte-exact golden tests for the yarn-classic / bun / yarn-berry patchers. Each golden was recorded from the
 * real package manager (yarn 1.22.22, bun 1.3.10, corepack yarn@4) for a closure-unchanged edit; the patcher
 * is invoked directly and its output asserted equal to the recorded {@code after} lock, byte for byte.
 */
class LockPatcherTest {

    // --- yarn classic --------------------------------------------------------

    @Test
    void yarnInPlaceBump() {
        PackageEdit edit = PackageEdit.builder()
                .name("left-pad")
                .oldVersion("1.2.0")
                .newVersion("1.3.0")
                .oldConstraint("1.2.0")
                .newResolved("https://registry.npmjs.org/left-pad/-/left-pad-1.3.0.tgz")
                .newShasum("5b8a3a7765dfe001261dde915589e782f8c94d1e")
                .newIntegrity("sha512-XI5MPzVNApjAyhQzphX8BkmKsKUxD4LdyK24iZeQGinBN9yTQT3bFlCBy/aVx2HrNcqQGsdot8ghrjyrvMCoEA==")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/yarn-classic/inplace/before"),
                Paths.get("yarn.lock"),
                PackageManager.YarnClassic,
                resource("/lock/yarn-classic/inplace/pkg-after"),
                singletonList(edit));

        assertThat(new YarnClassicLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/yarn-classic/inplace/after"));
    }

    @Test
    void yarnMergedHeaderSplit() {
        PackageEdit edit = PackageEdit.builder()
                .name("is-number")
                .oldVersion("6.0.0")
                .newVersion("7.0.0")
                .oldConstraint("~6.0.0")
                .newResolved("https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz")
                .newShasum("7535345b896734d5f80c4d06c50955527a14f12b")
                .newIntegrity("sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==")
                .newDependencies(Collections.emptyMap())
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/yarn-classic/split/before"),
                Paths.get("yarn.lock"),
                PackageManager.YarnClassic,
                resource("/lock/yarn-classic/split/pkg-after"),
                singletonList(edit));

        assertThat(new YarnClassicLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/yarn-classic/split/after"));
    }

    @Test
    void yarnInPlaceBumpKeepsNpmjsHost() {
        // A lock already on registry.npmjs.org must stay there — the host is mirrored from the siblings,
        // not force-rewritten to yarnpkg.com.
        PackageEdit edit = PackageEdit.builder()
                .name("left-pad")
                .oldVersion("1.2.0")
                .newVersion("1.3.0")
                .oldConstraint("1.2.0")
                .newResolved("https://registry.npmjs.org/left-pad/-/left-pad-1.3.0.tgz")
                .newShasum("5b8a3a7765dfe001261dde915589e782f8c94d1e")
                .newIntegrity("sha512-XI5MPzVNApjAyhQzphX8BkmKsKUxD4LdyK24iZeQGinBN9yTQT3bFlCBy/aVx2HrNcqQGsdot8ghrjyrvMCoEA==")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/yarn-classic/inplace-npmjs/before"),
                Paths.get("yarn.lock"),
                PackageManager.YarnClassic,
                resource("/lock/yarn-classic/inplace-npmjs/pkg-after"),
                singletonList(edit));

        assertThat(new YarnClassicLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/yarn-classic/inplace-npmjs/after"));
    }

    @Test
    void yarnRemoveLeaf() {
        PackageEdit edit = PackageEdit.builder()
                .name("left-pad")
                .oldVersion("1.3.0")
                .newVersion(null)
                .oldConstraint("1.3.0")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/yarn-classic/remove/before"),
                Paths.get("yarn.lock"),
                PackageManager.YarnClassic,
                resource("/lock/yarn-classic/remove/pkg-after"),
                singletonList(edit));

        assertThat(new YarnClassicLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/yarn-classic/remove/after"));
    }

    // --- bun -----------------------------------------------------------------

    @Test
    void bunLockRoundTripsByteExact() {
        String before = resource("/lock/bun/inplace/before");
        Parser.Input input = Parser.Input.fromString(Paths.get("bun.lock"), before);
        SourceFile sf = JsonParser.builder().build()
                .parseInputs(singletonList(input), null, new InMemoryExecutionContext())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(sf.printAll()).isEqualTo(before);
    }

    @Test
    void bunInPlaceBump() {
        PackageEdit edit = PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.0")
                .newVersion("3.0.1")
                .oldConstraint("3.0.0")
                .newIntegrity("sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/bun/inplace/before"),
                Paths.get("bun.lock"),
                PackageManager.Bun,
                resource("/lock/bun/inplace/pkg-after"),
                singletonList(edit));

        assertThat(new BunLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/bun/inplace/after"));
    }

    @Test
    void bunRemoveLeaf() {
        PackageEdit edit = PackageEdit.builder()
                .name("left-pad")
                .oldVersion("1.3.0")
                .newVersion(null)
                .oldConstraint("1.3.0")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/bun/remove/before"),
                Paths.get("bun.lock"),
                PackageManager.Bun,
                resource("/lock/bun/remove/pkg-after"),
                singletonList(edit));

        assertThat(new BunLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/bun/remove/after"));
    }

    @Test
    void bunNonLeafRemovalGCsPrivateTransitiveByteExact() {
        // Removing debug (non-leaf) must also drop its now-orphaned private transitive ms; golden recorded
        // from a real `bun install --lockfile-only` (bun 1.3.10).
        PackageEdit edit = PackageEdit.builder()
                .name("debug")
                .oldVersion("4.3.4")
                .newVersion(null)
                .oldConstraint("4.3.4")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                resource("/lock/bun/remove-nonleaf/before"),
                Paths.get("bun.lock"),
                PackageManager.Bun,
                resource("/lock/bun/remove-nonleaf/pkg-after"),
                singletonList(edit));

        assertThat(new BunLockPatcher().patch(edits))
                .isEqualTo(resource("/lock/bun/remove-nonleaf/after"));
    }

    // --- yarn berry (parse-only, fail loud) ----------------------------------

    @Test
    void yarnBerryFailsLoudChecksumUnavailable() {
        String before = resource("/lock/yarn-berry/basic/before");
        assertThat(before).contains("checksum:");

        PackageEdit edit = PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.1")
                .newVersion("3.0.2")
                .oldConstraint("^3.0.1")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                before,
                Paths.get("yarn.lock"),
                PackageManager.YarnBerry,
                resource("/lock/yarn-berry/basic/pkg-before"),
                singletonList(edit));

        assertThatThrownBy(() -> new YarnBerryLockPatcher().patch(edits))
                .isInstanceOfSatisfying(EngineFailure.class, ef -> {
                    assertThat(ef.failure.getReason()).isEqualTo(Reason.CHECKSUM_UNAVAILABLE);
                    assertThat(ef.failure.getPackageName()).isEqualTo("is-odd");
                });
    }

    @Test
    void malformedYarnFailsLoud() {
        LockEditSet edits = new LockEditSet(
                "{\"not\":\"a yarn lock\"}\n",
                Paths.get("yarn.lock"),
                PackageManager.YarnClassic,
                "{}",
                singletonList(PackageEdit.builder()
                        .name("left-pad").oldVersion("1.2.0").newVersion("1.3.0")
                        .oldConstraint("1.2.0").scope("dependencies").build()));

        assertThatThrownBy(() -> new YarnClassicLockPatcher().patch(edits))
                .isInstanceOfSatisfying(EngineFailure.class, ef ->
                        assertThat(ef.failure.getReason()).isEqualTo(Reason.MALFORMED_LOCK));
    }

    private static String resource(String path) {
        try (InputStream in = LockPatcherTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
