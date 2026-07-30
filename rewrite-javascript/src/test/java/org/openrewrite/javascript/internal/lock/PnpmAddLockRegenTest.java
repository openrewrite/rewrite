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
 * The differential harness for Phase B pnpm adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code pnpm-lock.yaml}, the recipe's add edit, and recorded registry HTTP —
 * through {@link NativeLockEngine} entirely OFFLINE (a stub {@link HttpSender} serves the captured
 * packuments/manifests), then asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code pnpm install --lockfile-only}. Byte-identity is the whole contract: the engine either
 * reproduces exactly what pnpm would write or fails loud.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealPnpm()}: it re-runs a real pnpm over every committed {@code pkg-before} and
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} — proving
 * both goldens are genuine pnpm output (pnpm reproduces {@code after} from {@code before + edit}). The minimal
 * packuments and verbatim single-version manifests under each fixture's {@code http/} were captured from the
 * registry (see {@code NpmClosureAddLockRegenTest} for the throwaway node script).
 */
class PnpmAddLockRegenTest {

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

    // --- byte-exact adds (goldens from real pnpm 11.2.2) ------------------

    @Test
    void leafAddV9() {
        // is-number@7.0.0 is a scalar-only leaf (resolution + engines); it inserts before the existing ms.
        assertAddByteExact("lock/pnpm/add-leaf", new String[][]{{"is-number", "7.0.0"}});
    }

    @Test
    void cleanClosureAddV9() {
        // is-odd@3.0.1 -> is-number@6.0.0: a clean two-member closure, no peers, no conflict. Each member gets
        // one packages+snapshots entry; is-odd's snapshot references the resolved is-number 6.0.0.
        assertAddByteExact("lock/pnpm/add-closure",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    // --- reverse-dependent content-fork (Phase B I5) ---------------------

    @Test
    void forkReverseDependentKeepsOldVersion() {
        // Root deps debug@2.6.9 (which pins ms@2.0.0) + ms. Bumping ms -> 2.1.3: pnpm never nests, so it adds
        // ms@2.1.3 content (packages+snapshots) and keeps ms@2.0.0 for debug, retargeting only the importer.
        // The engine reads debug's ms constraint from its manifest to prove debug excludes 2.1.3 (a fork, not
        // a dedupe). Routes: ms packument, debug@2.6.9 manifest (constraint), ms@2.1.3 manifest (new content).
        assertAddByteExact("lock/pnpm/fork", new String[][]{{"ms", "2.1.3"}, {"debug", "2.6.9"}});
    }

    @Test
    void forkWhenReverseDependentAcceptsNewVersionDefersAsDedupe() {
        // widget's snapshot resolves ms to 2.0.0, but its manifest range (^2.0.0) ACCEPTS the bumped 2.1.3 —
        // so pnpm would dedupe widget up to 2.1.3 (dropping ms@2.0.0), not fork. The engine reads the range
        // from widget's manifest and defers rather than emit a wrong content-fork.
        routes.put(REG + "ms", "{\"name\":\"ms\",\"dist-tags\":{},\"versions\":{\"2.0.0\":{},\"2.1.3\":{}}}");
        routes.put(REG + "widget/1.0.0",
                "{\"name\":\"widget\",\"version\":\"1.0.0\",\"dependencies\":{\"ms\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/widget/-/widget-1.0.0.tgz\",\"integrity\":\"sha512-W\"}}");

        String lock = "lockfileVersion: '9.0'\n\n" +
                "importers:\n\n  .:\n    dependencies:\n" +
                "      widget:\n        specifier: 1.0.0\n        version: 1.0.0\n" +
                "      ms:\n        specifier: 2.0.0\n        version: 2.0.0\n\n" +
                "packages:\n\n  widget@1.0.0:\n    resolution: {integrity: sha512-W}\n\n" +
                "  ms@2.0.0:\n    resolution: {integrity: sha512-M}\n\n" +
                "snapshots:\n\n  widget@1.0.0:\n    dependencies:\n      ms: 2.0.0\n\n  ms@2.0.0: {}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                "{\"dependencies\":{\"widget\":\"1.0.0\",\"ms\":\"2.1.3\"}}",
                "{\"dependencies\":{\"widget\":\"1.0.0\",\"ms\":\"2.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("ms");
        assertThat(result.getFailure().getDetail()).contains("dedupe");
    }

    // --- fail loud (any peer in the closure defers) -----------------------

    @Test
    void peerCarryingAddFailsLoud() {
        // pnpm encodes peers as suffix keys (pkg@ver(peer@ver)); the mechanical placement does not model them.
        routes.put(REG + "has-peer", "{\"name\":\"has-peer\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "has-peer/1.0.0",
                "{\"name\":\"has-peer\",\"version\":\"1.0.0\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/has-peer/-/has-peer-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PEER\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                editedPkg("has-peer", "^1.0.0"),
                resource("lock/pnpm/add-leaf/pkg-before"),
                resource("lock/pnpm/add-leaf/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependencies").contains("has-peer");
    }

    private static String editedPkg(String name, String range) {
        return "{\n  \"name\": \"g-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"" + name + "\": \"" + range + "\",\n    \"ms\": \"^2.1.3\"\n  }\n}\n";
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, version}} maps to two recorded routes: {@code http/<name>} (packument) and
     * {@code http/<name>-<version>} (manifest).
     */
    private void assertAddByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
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
        String[] fixtures = {"lock/pnpm/add-leaf", "lock/pnpm/add-closure", "lock/pnpm/fork"};
        for (String fixture : fixtures) {
            assertPnpmReproduces(fixture + "/pkg-before", fixture + "/before");
            assertPnpmReproduces(fixture + "/pkg-after", fixture + "/after");
        }
    }

    private void assertPnpmReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("pnpm-add-record");
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
        try (InputStream in = PnpmAddLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
