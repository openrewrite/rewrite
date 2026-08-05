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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Byte-exact regeneration of a multi-importer Yarn Berry {@code yarn.lock} when the edit lands on ONE workspace
 * member. The lock carries an {@code @workspace:} importer per member plus the root; a member-declared bump re-heads
 * only the shared {@code ms} entry and re-pins that member's importer range, leaving the root and every sibling
 * importer byte-identical. The engine runs OFFLINE (stub {@code HttpSender}, recorded packument/manifests/tarball);
 * goldens were recorded from a real {@code corepack yarn install} over a two-member workspace (see
 * {@link #recordGoldensWithRealYarn()}).
 */
class YarnBerryWorkspaceMemberLockRegenTest extends LockRegenTestSupport {

    @Test
    void memberBumpByteExact() {
        // packages/app declares ms; packages/lib declares is-buffer. Bumping ms 2.1.2 -> 2.1.3 in the app member
        // re-heads the ms entry (new descriptor/version/resolution/checksum) and re-pins only the app importer.
        String dir = "lock/yarn-berry/ws-member-bump";
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.1.2", resource(dir + "/http/ms-2.1.2"));
        routes.put(REG + "ms/2.1.3", resource(dir + "/http/ms-2.1.3"));
        binaryRoutes.put(REG + "ms/-/ms-2.1.3.tgz", bytesResource(dir + "/http/ms-2.1.3.tgz"));

        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg-app-after"),
                resource(dir + "/pkg-app-before"),
                resource(dir + "/before"),
                null, Paths.get("packages/app/package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    @Test
    void sharedAcrossMembersFailsLoud() {
        // before-shared: BOTH packages/app and packages/lib declare ms@npm:2.1.2, so they share one entry. A real
        // yarn 4.5.3 install forks (keeps ms@npm:2.1.2 for lib, adds ms@npm:2.1.3 for app); the minimal member-bump
        // path would instead re-head the shared entry and silently break lib, so the engine must refuse.
        String dir = "lock/yarn-berry/ws-member-bump";
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg-app-after"),
                resource(dir + "/pkg-app-before"),
                resource(dir + "/before-shared"),
                null, Paths.get("packages/app/package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
    }

    // --- live re-record / provenance check (disabled: needs corepack + network) ---

    @Test
    @Disabled("live: runs real yarn 4.5.3 via corepack over a two-member workspace to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertBerryWorkspaceReproduces("lock/yarn-berry/ws-member-bump", "pkg-app-before", "lock/yarn-berry/ws-member-bump/before");
        assertBerryWorkspaceReproduces("lock/yarn-berry/ws-member-bump", "pkg-app-after", "lock/yarn-berry/ws-member-bump/after");
    }

    /**
     * Materialize the two-member workspace ({@code pkg-root} + {@code workspace-yarnrc} + {@code packages/lib} from
     * {@code pkg-lib} + {@code packages/app} from {@code appManifest}), run a real {@code corepack yarn install},
     * and assert the emitted root lock equals {@code lockResource}.
     */
    private void assertBerryWorkspaceReproduces(String dir, String appManifest, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("berry-ws-regen-record");
        try {
            Files.write(tmp.resolve("package.json"), bytesResource(dir + "/pkg-root"));
            Files.write(tmp.resolve(".yarnrc.yml"), bytesResource(dir + "/workspace-yarnrc"));
            Path app = Files.createDirectories(tmp.resolve("packages").resolve("app"));
            Path lib = Files.createDirectories(tmp.resolve("packages").resolve("lib"));
            Files.write(app.resolve("package.json"), bytesResource(dir + "/" + appManifest));
            Files.write(lib.resolve("package.json"), bytesResource(dir + "/pkg-lib"));

            ProcessBuilder pb = new ProcessBuilder("corepack", "yarn", "install");
            pb.directory(tmp.toFile());
            pb.environment().put("COREPACK_ENABLE_DOWNLOAD_PROMPT", "0");
            pb.environment().put("YARN_GLOBAL_FOLDER", tmp.resolve(".yarn-global").toString());
            pb.redirectOutput(tmp.resolve("yarn.log").toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("yarn install timed out for " + dir + "/" + appManifest);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("yarn.lock")), StandardCharsets.UTF_8);
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
