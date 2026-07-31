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
 * The differential harness for Phase B pnpm orphan-prune: a direct-dependency bump whose new version DROPS a
 * {@code dependencies} edge. The bumped package's snapshot loses the dropped edge (an emptied snapshot becomes
 * {@code {}}) and every {@code packages}/{@code snapshots} entry the drop leaves unreachable is garbage-collected
 * — mirroring the npm orphan-prune (T13) for pnpm's content-addressed graph (v9). Each byte-exact test replays a
 * fixture entirely OFFLINE (a stub {@link HttpSender} serves the bumped package's captured packument/manifests;
 * the orphaned entries are read straight from the lock) through {@link NativeLockEngine} and asserts the emitted
 * lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code pnpm install --lockfile-only}.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealPnpm()}.
 */
class PnpmOrphanPruneLockRegenTest {

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

    // --- byte-exact orphan-prune bump (golden from real pnpm 11.2.2) ------

    @Test
    void orphanPruneV9() {
        // Bump semver 7.6.0 -> 7.8.5: semver 7.8.5 no longer depends on lru-cache, so semver's snapshot loses its
        // `lru-cache` edge (becoming {}) and lru-cache (plus its private transitive yallist) are GC'd from both
        // the packages and snapshots maps. semver's other metadata (engines/hasBin) is unchanged.
        assertOrphanPruneByteExact("lock/pnpm/orphan-prune",
                new String[][]{{"semver", "7.6.0", "7.8.5"}});
    }

    private void assertOrphanPruneByteExact(String dir, String[][] packages) {
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
        String fixture = "lock/pnpm/orphan-prune";
        assertPnpmReproduces(fixture + "/pkg-before", fixture + "/before");
        assertPnpmReproduces(fixture + "/pkg-after", fixture + "/after");
    }

    private void assertPnpmReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-orphan-record");
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
        try (InputStream in = PnpmOrphanPruneLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
