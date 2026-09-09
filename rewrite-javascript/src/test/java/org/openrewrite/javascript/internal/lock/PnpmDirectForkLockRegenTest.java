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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Differential harness for the pnpm direct-dependency disambiguation lever: a DIRECT dep that is unambiguous at
 * its importer used to defer the moment the SAME package appeared forked (nested/transitive) elsewhere at another
 * version, because the graph-wide version scan saw two versions. The bump now re-scopes to the version the
 * importer edge actually resolves and re-pins only that direct entry + importer edge, leaving the transitive fork
 * untouched — byte-exact against a real {@code pnpm install --lockfile-only}.
 * <p>
 * The in-place key rename stays byte-exact only when no other version of the same package sorts between old and
 * new; when it would ({@link #reorderingRenameFailsLoud()}) or when the direct dep is itself forked across
 * importers ({@link #genuineImporterForkFailsLoud()}), the engine defers rather than emit a reordered/ambiguous
 * lock. Goldens were produced with pnpm 11.2.2 against registry.npmjs.org; enable {@link #recordGoldensWithRealPnpm()}
 * to re-derive them.
 */
class PnpmDirectForkLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact direct-dep bump disambiguated from a transitive fork ---

    @Test
    void directBumpDisambiguatedFromTransitiveFork() {
        // The importer directly declares ms 2.0.0 while debug@4.3.4 pulls ms 2.1.2 nested — a graph-wide "ms is at
        // two versions" that used to defer. Bumping the direct ms 2.0.0 -> 2.1.1 re-pins only the importer edge and
        // renames ms@2.0.0 -> ms@2.1.1 in place (staying before the untouched transitive ms@2.1.2).
        String dir = "lock/pnpm/direct-fork";
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.0.0", resource(dir + "/http/ms-2.0.0"));
        routes.put(REG + "ms/2.1.1", resource(dir + "/http/ms-2.1.1"));

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- reordering in-place rename fails loud ----------------------------

    @Test
    void reorderingRenameFailsLoud() {
        // Same fork, but bump the direct ms 2.0.0 -> 2.1.3, which sorts AFTER the transitive ms@2.1.2. A real pnpm
        // reorders the two entries; the in-place rename would not, so the engine defers rather than emit a wrong lock.
        String dir = "lock/pnpm/direct-fork";
        routes.put(REG + "ms", resource(dir + "/http/ms"));

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                editedOneDep("ms", "2.1.3"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("ms");
        assertThat(result.getFailure().getDetail()).contains("reorder").contains("ms@2.1.2");
    }

    // --- genuine fork at the importer stays fail-loud ---------------------

    @Test
    void genuineImporterForkFailsLoud() {
        // Two importers both declare ms ^2.0.0 but resolve it to different versions (2.0.0 and 2.1.2). The direct
        // dep is genuinely forked at the importer, so the importer-scoped read is still ambiguous and the bump defers.
        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                editedOneDep("ms", "^2.1.1"),
                editedOneDep("ms", "^2.0.0"),
                twoImporterForkLock(), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("ms");
        assertThat(result.getFailure().getDetail()).contains("multiple versions");
    }

    private static String editedOneDep(String name, String range) {
        return "{\n  \"name\": \"c\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"" + name + "\": \"" + range + "\"\n  }\n}\n";
    }

    private static String twoImporterForkLock() {
        return "lockfileVersion: '9.0'\n\n" +
                "settings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
                "importers:\n\n" +
                "  .:\n    dependencies:\n" +
                "      ms:\n        specifier: ^2.0.0\n        version: 2.0.0\n\n" +
                "  packages/app:\n    dependencies:\n" +
                "      ms:\n        specifier: ^2.0.0\n        version: 2.1.2\n\n" +
                "packages:\n\n" +
                "  ms@2.0.0:\n    resolution: {integrity: sha512-MS0}\n\n" +
                "  ms@2.1.2:\n    resolution: {integrity: sha512-MS2}\n\n" +
                "snapshots:\n\n" +
                "  ms@2.0.0: {}\n\n" +
                "  ms@2.1.2: {}\n";
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        String fixture = "lock/pnpm/direct-fork";
        assertPnpmReproduces(fixture + "/pkg-before", fixture + "/before");
        assertPnpmReproduces(fixture + "/pkg-after", fixture + "/after");
    }
}
