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
 * Byte-exact regeneration of a multi-importer {@code bun.lock} when the edit lands on ONE workspace member. The
 * {@code workspaces} map holds one entry per member (keyed by directory) plus the root; a member-declared bump
 * re-pins only that member's constraint and the shared {@code packages} tuple, leaving the root and every sibling
 * importer byte-identical. The engine runs OFFLINE (stub {@code HttpSender}); goldens were recorded from a real
 * {@code bun install --lockfile-only} over a two-member workspace (see {@link #recordGoldensWithRealBun()}).
 */
class BunWorkspaceMemberLockRegenTest extends LockRegenTestSupport {

    @Test
    void memberBumpByteExact() {
        // packages/app declares ms; packages/lib declares is-buffer. Bumping ms 2.1.2 -> 2.1.3 in the app member
        // re-pins workspaces["packages/app"].dependencies.ms and the ms tuple's locator + integrity.
        String dir = "lock/bun/ws-member-bump";
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.1.2", resource(dir + "/http/ms-2.1.2"));
        routes.put(REG + "ms/2.1.3", resource(dir + "/http/ms-2.1.3"));

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource(dir + "/pkg-app-after"),
                resource(dir + "/pkg-app-before"),
                resource(dir + "/before"),
                null, Paths.get("packages/app/package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 over a two-member workspace to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        assertBunWorkspaceReproduces("lock/bun/ws-member-bump", "pkg-app-before", "lock/bun/ws-member-bump/before");
        assertBunWorkspaceReproduces("lock/bun/ws-member-bump", "pkg-app-after", "lock/bun/ws-member-bump/after");
    }

    /**
     * Materialize the two-member workspace ({@code pkg-root} + {@code packages/lib} from {@code pkg-lib} +
     * {@code packages/app} from {@code appManifest}), run a real {@code bun install --lockfile-only}, and assert the
     * emitted root lock equals {@code lockResource}.
     */
    private void assertBunWorkspaceReproduces(String dir, String appManifest, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("bun-ws-regen-record");
        try {
            Files.write(tmp.resolve("package.json"), bytesResource(dir + "/pkg-root"));
            Path app = Files.createDirectories(tmp.resolve("packages").resolve("app"));
            Path lib = Files.createDirectories(tmp.resolve("packages").resolve("lib"));
            Files.write(app.resolve("package.json"), bytesResource(dir + "/" + appManifest));
            Files.write(lib.resolve("package.json"), bytesResource(dir + "/pkg-lib"));

            Process process = new ProcessBuilder("bun", "install", "--lockfile-only", "--no-progress")
                    .directory(tmp.toFile())
                    .redirectOutput(tmp.resolve("bun.log").toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("bun install timed out for " + dir + "/" + appManifest);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("bun.lock")), StandardCharsets.UTF_8);
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
