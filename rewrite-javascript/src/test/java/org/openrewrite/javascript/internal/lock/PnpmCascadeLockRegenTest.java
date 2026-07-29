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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for Phase B pnpm cascade upgrades: a direct-dependency version bump whose new
 * version's {@code dependencies} constraint on a shared transitive changed such that the transitive must MOVE
 * to a new version. pnpm keys entries by resolved version, so the move renames the moved package's
 * {@code packages}/{@code snapshots} keys ({@code @old}→{@code @new}, new integrity) and retargets every
 * snapshot's resolved reference. Each byte-exact test replays a fixture entirely OFFLINE (a stub
 * {@link HttpSender} serves captured packuments/manifests) through {@link NativeLockEngine} and asserts the
 * emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code pnpm install
 * --lockfile-only}.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealPnpm()}: it re-runs a real pnpm over the committed {@code pkg-before}/
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} — proving
 * both are genuine pnpm output (pnpm reproduces {@code after} from {@code before + bump}). The recorded HTTP
 * captures under {@code http/} are the same verbatim registry packuments/manifests as {@code
 * NpmCascadeLockRegenTest}'s debug→ms cascade; only the placement (content-addressed vs hoisted) differs.
 */
class PnpmCascadeLockRegenTest {

    private static final String REG = "https://registry.npmjs.org/";

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    // --- byte-exact cascade upgrade (golden from real pnpm 11.2.2) --------

    @Test
    void basicCascadeV9() {
        // Bump debug ^2.6.9 -> ^3.0.0: debug's `ms` edge moves 2.0.0 -> ^2.1.1, forcing the private leaf ms to
        // move 2.0.0 -> 2.1.3. debug's importer + packages/snapshots entries move, ms's packages/snapshots keys
        // rename with new integrity, and debug's snapshot reference `ms: 2.0.0` retargets to `ms: 2.1.3`.
        assertCascadeByteExact("lock/pnpm/cascade",
                new String[][]{{"debug", "2.6.9", "3.2.7"}, {"ms", "2.0.0", "2.1.3"}});
    }

    // --- sideways cascade (shared transitive) fails loud ------------------

    @Test
    void sidewaysCascadeFailsLoud() {
        // alpha 1.0.0 -> 2.0.0 changes its `shared` edge ^1.0.0 -> ^2.0.0, forcing shared off 1.5.0. But sibling
        // beta also references shared 1.5.0, and pnpm records resolved versions (not beta's range), so the move
        // cannot prove beta accepts the new shared. The resolver refuses rather than risk a fork.
        routes.put(REG + "alpha", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0",
                "{\"name\":\"alpha\",\"version\":\"1.0.0\",\"dependencies\":{\"shared\":\"^1.0.0\"}}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\",\"dependencies\":{\"shared\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                editedTwoDep("alpha", "^2.0.0", "beta", "^1.0.0"),
                editedTwoDep("alpha", "^1.0.0", "beta", "^1.0.0"),
                sharedTransitiveLock(), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("shared");
        assertThat(result.getFailure().getDetail()).contains("shared").contains("beta@1.0.0");
    }

    // --- peer-carrying mover fails loud (pnpm suffix keys unmodeled) ------

    @Test
    void peerCarryingMoverFailsLoud() {
        // host 1.0.0 -> 2.0.0 forces its private transitive plugin off 1.0.0. plugin declares a peerDependency
        // (identical across versions, so the surface proof passes) — pnpm encodes peers as suffix keys the
        // mechanical retarget does not model, so the move defers.
        routes.put(REG + "host", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "host/1.0.0",
                "{\"name\":\"host\",\"version\":\"1.0.0\",\"dependencies\":{\"plugin\":\"^1.0.0\"}}");
        routes.put(REG + "host/2.0.0",
                "{\"name\":\"host\",\"version\":\"2.0.0\",\"dependencies\":{\"plugin\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/host/-/host-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-HOST2\"}}");
        routes.put(REG + "plugin", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "plugin/1.0.0",
                "{\"name\":\"plugin\",\"version\":\"1.0.0\",\"peerDependencies\":{\"react\":\">=17\"}}");
        routes.put(REG + "plugin/2.0.0",
                "{\"name\":\"plugin\",\"version\":\"2.0.0\",\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/plugin/-/plugin-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PLUGIN2\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                editedOneDep("host", "^2.0.0"),
                editedOneDep("host", "^1.0.0"),
                peerMoverLock(), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("plugin");
        assertThat(result.getFailure().getDetail()).contains("peerDependencies");
    }

    private static String editedOneDep(String name, String range) {
        return "{\n  \"name\": \"c\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"" + name + "\": \"" + range + "\"\n  }\n}\n";
    }

    private static String editedTwoDep(String a, String aRange, String b, String bRange) {
        return "{\n  \"name\": \"c\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"" + a + "\": \"" + aRange + "\",\n    \"" + b + "\": \"" + bRange + "\"\n  }\n}\n";
    }

    private static String sharedTransitiveLock() {
        return "lockfileVersion: '9.0'\n\n" +
                "settings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
                "importers:\n\n  .:\n    dependencies:\n" +
                "      alpha:\n        specifier: ^1.0.0\n        version: 1.0.0\n" +
                "      beta:\n        specifier: ^1.0.0\n        version: 1.0.0\n\n" +
                "packages:\n\n" +
                "  alpha@1.0.0:\n    resolution: {integrity: sha512-ALPHA1}\n\n" +
                "  beta@1.0.0:\n    resolution: {integrity: sha512-BETA1}\n\n" +
                "  shared@1.5.0:\n    resolution: {integrity: sha512-SHARED}\n\n" +
                "snapshots:\n\n" +
                "  alpha@1.0.0:\n    dependencies:\n      shared: 1.5.0\n\n" +
                "  beta@1.0.0:\n    dependencies:\n      shared: 1.5.0\n\n" +
                "  shared@1.5.0: {}\n";
    }

    private static String peerMoverLock() {
        return "lockfileVersion: '9.0'\n\n" +
                "settings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\n" +
                "importers:\n\n  .:\n    dependencies:\n" +
                "      host:\n        specifier: ^1.0.0\n        version: 1.0.0\n\n" +
                "packages:\n\n" +
                "  host@1.0.0:\n    resolution: {integrity: sha512-HOST1}\n\n" +
                "  plugin@1.0.0:\n    resolution: {integrity: sha512-PLUGIN1}\n\n" +
                "snapshots:\n\n" +
                "  host@1.0.0:\n    dependencies:\n      plugin: 1.0.0\n\n" +
                "  plugin@1.0.0: {}\n";
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, v1, v2}} maps to a packument route ({@code http/<name>}) and one manifest route per
     * version ({@code http/<name>-<version>}) — both the old (bumped-from) and new (bumped-to) manifests.
     */
    private void assertCascadeByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            for (int i = 1; i < pkg.length; i++) {
                routes.put(route + "/" + pkg[i], resource(dir + "/http/" + pkg[0] + "-" + pkg[i]));
            }
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        String fixture = "lock/pnpm/cascade";
        assertPnpmReproduces(fixture + "/pkg-before", fixture + "/before");
        assertPnpmReproduces(fixture + "/pkg-after", fixture + "/after");
    }

    private void assertPnpmReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-cascade-record");
        try {
            Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
            Process process = new ProcessBuilder("pnpm", "install", "--lockfile-only", "--no-color")
                    .directory(tmp.toFile())
                    .redirectOutput(tmp.resolve("pnpm.log").toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("pnpm install timed out for " + pkgResource);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("pnpm-lock.yaml")), StandardCharsets.UTF_8);
            assertThat(generated).as(pkgResource + " -> " + lockResource).isEqualTo(resource(lockResource));
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    private static String resource(String path) {
        try (InputStream in = PnpmCascadeLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
