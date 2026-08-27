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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Byte-exact regeneration of a monorepo pnpm lock (lockfileVersion 9) when the edit lands on ONE
 * workspace member. A single root {@code pnpm-lock.yaml} carries an {@code importers:} entry per member;
 * a member-declared change re-pins only that member's importer, leaving the root and every sibling member
 * untouched, and reconciles {@code packages}/{@code snapshots}. The engine runs OFFLINE (stub
 * {@code HttpSender}); goldens were recorded from a real {@code pnpm install --lockfile-only} over a
 * two-member workspace (see {@link #recordGoldensWithRealPnpm()}).
 */
class PnpmWorkspaceMemberLockRegenTest extends LockRegenTestSupport {

    @Test
    void memberBumpByteExact() {
        // packages/app declares ms; packages/lib declares is-buffer. Bumping ms 2.1.2 -> 2.1.3 in the app
        // member re-pins only the packages/app importer plus ms's packages/snapshots entries.
        routes.put(REG + "ms", resource("lock/pnpm/v9/http/ms"));
        routes.put(REG + "ms/2.1.2", resource("lock/pnpm/v9/http/ms-2.1.2"));
        routes.put(REG + "ms/2.1.3", resource("lock/pnpm/v9/http/ms-2.1.3"));

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource("lock/pnpm/v9-ws/pkg-app-after"),
                resource("lock/pnpm/v9-ws/pkg-app-before"),
                resource("lock/pnpm/v9-ws/before"),
                null, Paths.get("packages/app/package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/pnpm/v9-ws/after"));
    }

    @Test
    void memberAddByteExact() {
        // A brand-new leaf (object-assign) added to the packages/app member: a new importer edge under
        // packages/app plus one packages+snapshots entry; the root "." and packages/lib stay byte-identical.
        routes.put(REG + "object-assign", resource("lock/pnpm/add-ws-member/http/object-assign"));
        routes.put(REG + "object-assign/4.1.1", resource("lock/pnpm/add-ws-member/http/object-assign-4.1.1"));

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource("lock/pnpm/add-ws-member/pkg-app-after"),
                resource("lock/pnpm/add-ws-member/pkg-app-before"),
                resource("lock/pnpm/add-ws-member/before"),
                null, Paths.get("packages/app/package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/pnpm/add-ws-member/after"));
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the workspace goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        assertPnpmWorkspaceReproduces("lock/pnpm/v9-ws", "pkg-app-before", "lock/pnpm/v9-ws/before");
        assertPnpmWorkspaceReproduces("lock/pnpm/v9-ws", "pkg-app-after", "lock/pnpm/v9-ws/after");
        assertPnpmWorkspaceReproduces("lock/pnpm/add-ws-member", "pkg-app-before", "lock/pnpm/add-ws-member/before");
        assertPnpmWorkspaceReproduces("lock/pnpm/add-ws-member", "pkg-app-after", "lock/pnpm/add-ws-member/after");
    }

    /**
     * Materialize the two-member workspace ({@code pkg-root} + {@code workspace-yaml} + {@code packages/lib}
     * from {@code pkg-lib} + {@code packages/app} from {@code appManifestResource}), run a real
     * {@code pnpm install --lockfile-only}, and assert the emitted root lock equals {@code lockResource}.
     */
    private void assertPnpmWorkspaceReproduces(String dir, String appManifest, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-ws-regen-record");
        try {
            Files.write(tmp.resolve("package.json"), bytesResource(dir + "/pkg-root"));
            Files.write(tmp.resolve("pnpm-workspace.yaml"), bytesResource(dir + "/workspace-yaml"));
            Path app = Files.createDirectories(tmp.resolve("packages").resolve("app"));
            Path lib = Files.createDirectories(tmp.resolve("packages").resolve("lib"));
            Files.write(app.resolve("package.json"), bytesResource(dir + "/" + appManifest));
            Files.write(lib.resolve("package.json"), bytesResource(dir + "/pkg-lib"));

            Process process = new ProcessBuilder("pnpm", "install", "--lockfile-only", "--no-color")
                    .directory(tmp.toFile())
                    .redirectOutput(tmp.resolve("pnpm.log").toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("pnpm install timed out for " + dir + "/" + appManifest);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("pnpm-lock.yaml")), StandardCharsets.UTF_8);
            assertThat(generated).as(dir + "/" + appManifest + " -> " + lockResource).isEqualTo(resource(lockResource));
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }
}
