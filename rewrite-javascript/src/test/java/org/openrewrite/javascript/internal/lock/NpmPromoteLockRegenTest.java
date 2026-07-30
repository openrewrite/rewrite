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
 * The differential harness for promoting an already-installed transitive to a declared dependency. When the
 * recipe adds a dependency that is already in the tree as a satisfying transitive, npm keeps the install entry
 * untouched and only writes the importer's declared constraint (creating the {@code dependencies} block when the
 * root previously had only {@code devDependencies}); a dev-only transitive promoted to production also has its
 * {@code "dev": true} cleared. Each test replays a fixture through {@link NativeLockEngine} entirely OFFLINE and
 * asserts the emitted lock is BYTE-IDENTICAL to a golden recorded from real {@code npm install}. The promotion
 * path resolves only the added constraint (packument), so no manifest route is needed.
 * <p>
 * The goldens were produced with npm 11.6.2 against registry.npmjs.org; {@link #recordGoldensWithRealNpm()}
 * re-derives and verifies them.
 */
class NpmPromoteLockRegenTest {

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

    @Test
    void promoteProdTransitive() {
        // humanize-ms pulls ms@2.1.3 as a prod transitive; adding ms@^2.1.3 directly only writes the
        // importer edge (sorted after humanize-ms), leaving the ms install entry byte-identical.
        assertPromoteByteExact("lock/npm/promote-prod", "ms");
    }

    @Test
    void promoteDevTransitiveToProduction() {
        // ms is a dev-only transitive of a devDependency; adding it to `dependencies` creates the
        // `dependencies` block (before `devDependencies`) and clears "dev": true on the ms entry.
        assertPromoteByteExact("lock/npm/promote-dev", "ms");
    }

    @Test
    void promoteProdTransitiveV2() {
        // The same prod promotion into a lockfileVersion 2 lock: npm changes only the importer edge; the
        // legacy `dependencies` tree is untouched (no dev flag involved).
        assertPromoteByteExact("lock/npm/promote-prod-v2", "ms");
    }

    @Test
    void promoteDevToProductionV2FailsLoud() {
        // A v2 dev→prod promotion clears "dev": true in two places (the packages entry AND the legacy tree);
        // the second writer is not yet verified, so defer rather than emit a lock missing the legacy clear.
        routes.put(REG + "ms", resource("lock/npm/promote-dev-v2/http/ms"));

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/promote-dev-v2/pkg-after"),
                resource("lock/npm/promote-dev-v2/pkg-before"),
                resource("lock/npm/promote-dev-v2/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("lockfileVersion 2 promotion");
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Only the added dependency's packument ({@code http/<name>}) is served — a promotion resolves the added
     * constraint but reuses the installed entry, so no manifest is fetched.
     */
    private void assertPromoteByteExact(String dir, String addedName) {
        routes.put(REG + addedName.replace("/", "%2F"), resource(dir + "/http/" + addedName));

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
        // {fixture dir, lockfileVersion}.
        String[][] fixtures = {
                {"lock/npm/promote-prod", "3"},
                {"lock/npm/promote-dev", "3"},
                {"lock/npm/promote-prod-v2", "2"},
                {"lock/npm/promote-dev-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }

    private void assertNpmReproduces(String pkgResource, String lockResource, String lockfileVersion) throws Exception {
        Path tmp = Files.createTempDirectory("promote-record");
        try {
            Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
            Process process = new ProcessBuilder("npm", "install", "--package-lock-only",
                    "--lockfile-version", lockfileVersion, "--ignore-scripts", "--no-audit", "--no-fund")
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
        try (InputStream in = NpmPromoteLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
