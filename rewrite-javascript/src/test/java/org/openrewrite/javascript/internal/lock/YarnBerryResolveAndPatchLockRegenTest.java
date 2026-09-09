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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for Yarn Berry whole-closure resolve-and-patch. Each fixture is a real incremental
 * install with yarn 4.5.3 ({@code cacheKey: 10c0}): {@code pkg-before} installed from an empty lock gives the
 * golden {@code before}, the edited {@code pkg} re-installed in the same directory gives {@code after}. Each test
 * replays that edit through the whole {@code NativeLockEngine.regenerate(...)} entirely OFFLINE (packuments,
 * manifests, and tarball bytes under {@code http/}): the per-dependency proof defers, the engine resolves the
 * closure seeded by the before lock, diffs it, patches, and must reproduce {@code after} BYTE-IDENTICAL — fresh
 * entries' checksums reproduced from the tarballs, untouched entries' checksums preserved without a fetch.
 */
class YarnBerryResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void wholeManifestReconcileIntoRootOnlyLock() {
        // No pre-edit manifest to scope the per-dependency proof: the whole manifest resolves against the
        // root-only before lock and every declared dependency plus its closure lands as a fresh entry.
        assertResolveAndPatch("lock/yarn-berry/resolve-clean", null,
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void devAndOptionalAdds() {
        // once (prod) is retained; the edit adds supports-color (dev) and is-odd (optional). Both merge into the
        // workspace entry's single dependencies block, and is-odd is flagged in a fresh dependenciesMeta block.
        assertResolveAndPatch("lock/yarn-berry/resolve-dev-optional", "pkg-before",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void satisfiedPeerAdd() {
        // react is retained (checksum untouched, no tarball fetched); the added use-sync-external-store entry
        // records its peerDependencies block verbatim with yarn's unquoted range.
        assertResolveAndPatch("lock/yarn-berry/resolve-peer", "pkg-before",
                new String[][]{{"react", "19.0.0"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerWithMetaAdd() {
        // styled-jsx adds dependencies + a quoted peerDependencies range + peerDependenciesMeta (scoped keys).
        assertResolveAndPatch("lock/yarn-berry/resolve-peer-meta", "pkg-before",
                new String[][]{{"react", "19.0.0"}, {"styled-jsx", "5.1.6"}, {"client-only", "0.0.1"}});
    }

    @Test
    void unsupportedCacheKeyFailsLoud() {
        // Only the 10c0 zip format is validated; a fresh entry needing a checksum under any other cacheKey defers.
        String dir = "lock/yarn-berry/resolve-clean";
        routeAll(dir, new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
        String before = resource(dir + "/before").replace("cacheKey: 10c0", "cacheKey: 9");
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg"), null, before, null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.CHECKSUM_UNAVAILABLE);
    }

    private void assertResolveAndPatch(String dir, @Nullable String pkgBefore, String[][] packages) {
        routeAll(dir, packages);
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg"), pkgBefore == null ? null : resource(dir + "/" + pkgBefore),
                resource(dir + "/before"), null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    /** Each distinct name maps to a packument, single-version manifest, and the tarball bytes for its checksum. */
    private void routeAll(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
            binaryRoutes.put(REG + pkg[0] + "/-/" + pkg[0] + "-" + pkg[1] + ".tgz",
                    bytesResource(dir + "/http/" + pkg[0] + "-" + pkg[1] + ".tgz"));
        }
    }

    // --- live re-record / provenance check (disabled: needs corepack + network) ---

    @Test
    @Disabled("live: runs real yarn 4.5.3 via corepack to re-derive and verify the incremental goldens")
    void recordGoldensWithRealYarn() throws Exception {
        recordIncremental("lock/yarn-berry/resolve-clean");
        recordIncremental("lock/yarn-berry/resolve-dev-optional");
        recordIncremental("lock/yarn-berry/resolve-peer");
        recordIncremental("lock/yarn-berry/resolve-peer-meta");
    }

    /** Two-phase: yarn on {@code pkg-before} must reproduce {@code before}, then yarn on {@code pkg} {@code after}. */
    private void recordIncremental(String dir) throws Exception {
        Path tmp = Files.createTempDirectory("berry-resolve-record");
        try {
            Files.write(tmp.resolve(".yarnrc.yml"),
                    "nodeLinker: node-modules\nenableTelemetry: false\n".getBytes(StandardCharsets.UTF_8));
            Files.write(tmp.resolve("yarn.lock"), new byte[0]);
            yarnInstallInto(tmp, dir + "/pkg-before", dir + "/before");
            yarnInstallInto(tmp, dir + "/pkg", dir + "/after");
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    private void yarnInstallInto(Path tmp, String pkgResource, String lockResource) throws Exception {
        Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
        ProcessBuilder pb = new ProcessBuilder("corepack", "yarn", "install", "--mode", "update-lockfile");
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
    }
}
