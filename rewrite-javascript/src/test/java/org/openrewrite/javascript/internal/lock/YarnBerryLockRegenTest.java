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
 * The differential harness for Yarn Berry bumps. Each test replays a fixture — a before {@code package.json},
 * a before {@code yarn.lock}, the recipe's version bump, and recorded registry HTTP (packument, both version
 * manifests, and the new version's tarball) — through {@link NativeLockEngine} entirely OFFLINE, then asserts
 * the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code yarn install}.
 * <p>
 * Reproducing the {@code checksum} is what makes berry patchable at all: the engine fetches the tarball and
 * {@link BerryZipChecksum} rebuilds yarn's normalized zip to hash it. The goldens were produced with yarn 4.5.3
 * ({@code cacheKey: 10c0}); {@link #recordGoldensWithRealYarn()} re-derives and verifies them via corepack.
 */
class YarnBerryLockRegenTest extends LockRegenTestSupport {

    @Test
    void leafBump() {
        // ms 2.1.2 -> 2.1.3: a leaf whose descriptor key, version, resolution and checksum all move, plus the
        // importer's declared range. The checksum is the reproduced 10c0/d924b57e... zip hash.
        String dir = "lock/yarn-berry/leaf-bump";
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.1.2", resource(dir + "/http/ms-2.1.2"));
        routes.put(REG + "ms/2.1.3", resource(dir + "/http/ms-2.1.3"));
        binaryRoutes.put(REG + "ms/-/ms-2.1.3.tgz", bytesResource(dir + "/http/ms-2.1.3.tgz"));

        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg-after"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    @Test
    void unsupportedCacheKeyFailsLoud() {
        // Only the 10c0 zip format is validated; any other cacheKey cannot be reproduced, so refuse.
        String dir = "lock/yarn-berry/leaf-bump";
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.1.2", resource(dir + "/http/ms-2.1.2"));
        routes.put(REG + "ms/2.1.3", resource(dir + "/http/ms-2.1.3"));
        binaryRoutes.put(REG + "ms/-/ms-2.1.3.tgz", bytesResource(dir + "/http/ms-2.1.3.tgz"));
        String before = resource(dir + "/before").replace("cacheKey: 10c0", "cacheKey: 9");

        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg-after"), resource(dir + "/pkg-before"), before,
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.CHECKSUM_UNAVAILABLE);
    }

    // --- live re-record / provenance check (disabled: needs corepack + network) ---

    @Test
    @Disabled("live: runs real yarn 4.5.3 via corepack to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertBerryReproduces("lock/yarn-berry/leaf-bump/pkg-before", "lock/yarn-berry/leaf-bump/before");
        assertBerryReproduces("lock/yarn-berry/leaf-bump/pkg-after", "lock/yarn-berry/leaf-bump/after");
    }

    private void assertBerryReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("berry-regen-record");
        try {
            Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
            Files.write(tmp.resolve(".yarnrc.yml"),
                    "nodeLinker: node-modules\nenableTelemetry: false\n".getBytes(StandardCharsets.UTF_8));
            ProcessBuilder pb = new ProcessBuilder("corepack", "yarn", "install");
            pb.directory(tmp.toFile());
            pb.environment().put("COREPACK_ENABLE_DOWNLOAD_PROMPT", "0");
            pb.environment().put("YARN_GLOBAL_FOLDER", tmp.resolve(".yarn-global").toString());
            pb.redirectOutput(tmp.resolve("yarn.log").toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("yarn install timed out for " + pkgResource);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("yarn.lock")), StandardCharsets.UTF_8);
            assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
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
