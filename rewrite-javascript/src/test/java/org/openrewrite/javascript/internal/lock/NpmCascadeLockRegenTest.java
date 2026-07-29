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
 * The differential harness for Phase B I3 npm cascade upgrades: a direct-dependency version bump whose new
 * version's {@code dependencies} constraint on a shared transitive changed such that the transitive must
 * MOVE to a new version. Each byte-exact test replays a fixture entirely OFFLINE (a stub {@link HttpSender}
 * serves captured packuments/manifests) through {@link NativeLockEngine} and asserts the emitted lock is
 * BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2. To re-derive/verify them, enable {@link #recordGoldensWithRealNpm()}:
 * it re-runs a real npm over every committed {@code pkg-before}/{@code pkg-after} and asserts the resulting
 * lock equals the committed {@code before}/{@code after}, proving both are genuine npm output (npm reproduces
 * {@code after} from {@code before + bump}). Fixtures were captured with the same node script documented in
 * {@link NpmClosureAddLockRegenTest}.
 */
class NpmCascadeLockRegenTest {

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

    // --- byte-exact cascade upgrades (goldens from real npm 11.6.2) -------

    @Test
    void basicCascadeV3() {
        // Bump debug ^2.6.9 -> ^3.0.0: debug's `ms` constraint moves 2.0.0 -> ^2.1.1, forcing the shared
        // leaf ms to move 2.0.0 -> 2.1.3. No other reshaping (ms is a leaf with identical metadata).
        assertCascadeByteExact("lock/npm/cascade-basic",
                new String[][]{{"debug", "2.6.9", "3.2.7"}, {"ms", "2.0.0", "2.1.3"}});
    }

    @Test
    void basicCascadeV2() {
        // The same cascade into a lockfileVersion 2 lock: the packages entries AND the legacy `dependencies`
        // tree (debug's `requires.ms` re-pinned, ms's minimal entry moved) update together.
        assertCascadeByteExact("lock/npm/cascade-basic-v2",
                new String[][]{{"debug", "2.6.9", "3.2.7"}, {"ms", "2.0.0", "2.1.3"}});
    }

    // --- sideways cascade (reverse-edge conflict) fails loud --------------

    @Test
    void sidewaysCascadeFailsLoud() {
        // alpha 1.0.0 -> 2.0.0 changes its `shared` edge ^1.0.0 -> ^2.0.0, forcing shared to move off 1.5.0.
        // But sibling beta pins shared at 1.5.0 exactly — no single version satisfies both, so npm would
        // fork/nest shared. The greedy-forward resolver refuses rather than reshape (a sideways cascade).
        routes.put(REG + "alpha", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0",
                "{\"name\":\"alpha\",\"version\":\"1.0.0\",\"dependencies\":{\"shared\":\"^1.0.0\"}}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\",\"dependencies\":{\"shared\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");
        routes.put(REG + "shared", "{\"versions\":{\"1.5.0\":{},\"2.0.0\":{}}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\", \"beta\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\", \"dependencies\": {\"shared\": \"^1.0.0\"}},\n" +
                "    \"node_modules/beta\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\", \"integrity\": \"sha512-BETA1\", \"dependencies\": {\"shared\": \"1.5.0\"}},\n" +
                "    \"node_modules/shared\": {\"version\": \"1.5.0\", \"resolved\": \"https://registry.npmjs.org/shared/-/shared-1.5.0.tgz\", \"integrity\": \"sha512-SHARED\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^2.0.0\",\"beta\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\",\"beta\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("shared");
        assertThat(result.getFailure().getDetail()).contains("no single version of shared satisfies");
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
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs npm + network) ---

    @Test
    @Disabled("live: runs real npm 11.6.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealNpm() throws Exception {
        String[][] fixtures = {
                {"lock/npm/cascade-basic", "3"},
                {"lock/npm/cascade-basic-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }

    private void assertNpmReproduces(String pkgResource, String lockResource, String lockfileVersion) throws Exception {
        Path tmp = Files.createTempDirectory("cascade-record");
        try {
            Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
            Process process = new ProcessBuilder("npm", "install", "--package-lock-only",
                    "--lockfile-version", lockfileVersion, "--no-audit", "--no-fund")
                    .directory(tmp.toFile())
                    .redirectOutput(tmp.resolve("npm.log").toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("npm install timed out for " + pkgResource);
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("package-lock.json")), StandardCharsets.UTF_8);
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
        try (InputStream in = NpmCascadeLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
