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
 * The differential harness for Phase B yarn-classic adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code yarn.lock}, the recipe's add edit, and recorded registry HTTP —
 * through {@link NativeLockEngine} entirely OFFLINE (a stub {@link HttpSender} serves the captured
 * packuments/manifests), then asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code yarn install}. Byte-identity is the whole contract: the engine either reproduces exactly
 * what yarn would write or fails loud.
 * <p>
 * The goldens were produced with yarn 1.22.22 against registry.yarnpkg.com. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealYarn()}: it re-runs a real yarn over every committed {@code pkg-before} and
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} — proving
 * both goldens are genuine yarn output (yarn reproduces {@code after} from {@code before + edit}). The minimal
 * packuments and verbatim single-version manifests under each fixture's {@code http/} are the same registry
 * responses the bun/npm harnesses capture (yarn mirrors the tarball host to registry.yarnpkg.com itself).
 */
class YarnClassicAddLockRegenTest {

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

    // --- byte-exact adds (goldens from real yarn 1.22.22) -----------------

    @Test
    void leafAdd() {
        // is-number@7.0.0 is a leaf (its engines/bin never reach a yarn.lock block); it inserts before ms.
        assertAddByteExact("lock/yarn-classic/add-leaf", new String[][]{{"is-number", "7.0.0"}});
    }

    @Test
    void cleanClosureAdd() {
        // is-odd@3.0.1 -> is-number@6.0.0: a clean two-member closure. is-odd's block carries a dependencies
        // section (is-number "^6.0.0"); is-number's block header takes that range as its sole selector.
        assertAddByteExact("lock/yarn-classic/add-closure",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    // --- fail loud (a peer in the closure, or a transitive conflict, defers) ---

    @Test
    void peerCarryingAddFailsLoud() {
        // yarn resolves peers into further blocks the clean placement does not model, so any peer defers.
        routes.put(REG + "has-peer", "{\"name\":\"has-peer\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "has-peer/1.0.0",
                "{\"name\":\"has-peer\",\"version\":\"1.0.0\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"shasum\":\"1111111111111111111111111111111111111111\"," +
                        "\"tarball\":\"https://registry.npmjs.org/has-peer/-/has-peer-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PEER\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                editedPkg("has-peer", "^1.0.0"),
                resource("lock/yarn-classic/add-leaf/pkg-before"),
                resource("lock/yarn-classic/add-leaf/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependencies").contains("has-peer");
    }

    @Test
    void conflictingTransitiveAddFailsLoud() {
        // Adding is-odd (needs is-number ^6.0.0) into a lock that already has an is-number@7.0.0 block: yarn
        // would fork a second is-number block, the merged-selector/dedup nuance the clean placement refuses.
        routes.put(REG + "is-odd", resource("lock/yarn-classic/add-closure/http/is-odd"));
        routes.put(REG + "is-odd/3.0.1", resource("lock/yarn-classic/add-closure/http/is-odd-3.0.1"));

        String original = resource("lock/yarn-classic/add-leaf/pkg-after"); // is-number ^7.0.0, ms ^2.1.3
        String edited = "{\n  \"name\": \"yarn-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"is-number\": \"^7.0.0\",\n    \"is-odd\": \"^3.0.1\",\n    \"ms\": \"^2.1.3\"\n  }\n}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                edited, original, resource("lock/yarn-classic/add-leaf/after"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-number").contains("already present");
    }

    private static String editedPkg(String name, String range) {
        return "{\n  \"name\": \"yarn-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
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
        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs yarn + network) ---

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        String[] fixtures = {"lock/yarn-classic/add-leaf", "lock/yarn-classic/add-closure"};
        for (String fixture : fixtures) {
            assertYarnReproduces(fixture + "/pkg-before", fixture + "/before");
            assertYarnReproduces(fixture + "/pkg-after", fixture + "/after");
        }
    }

    private void assertYarnReproduces(String pkgResource, String lockResource) throws Exception {
        Path tmp = Files.createTempDirectory("yarn-add-record");
        try {
            Files.write(tmp.resolve("package.json"), resource(pkgResource).getBytes(StandardCharsets.UTF_8));
            ProcessBuilder pb = new ProcessBuilder("yarn", "install", "--ignore-scripts", "--non-interactive", "--no-progress");
            pb.directory(tmp.toFile());
            pb.environment().put("YARN_CACHE_FOLDER", tmp.resolve(".cache").toString());
            pb.redirectOutput(tmp.resolve("yarn.log").toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
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
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    private static String resource(String path) {
        try (InputStream in = YarnClassicAddLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
