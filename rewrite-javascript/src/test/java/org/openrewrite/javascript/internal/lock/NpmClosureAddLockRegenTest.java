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
 * The differential harness for Phase B npm closure adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code package-lock.json}, the recipe's add edit, and recorded registry
 * HTTP — through {@link NativeLockEngine} entirely OFFLINE (a stub {@link HttpSender} serves the captured
 * packuments/manifests), then asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after}
 * recorded from a real {@code npm install --package-lock-only}. Byte-identity is the whole contract: the
 * engine either reproduces exactly what npm would write or fails loud.
 * <p>
 * The goldens were produced with npm 11.6.2 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealNpm()}: it re-runs a real npm over every committed {@code pkg-before} and
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} —
 * proving both goldens are genuine npm output and that npm reproduces {@code after} from
 * {@code before + edit}. The minimal packuments ({@code {name, dist-tags, versions:{...:{}}}}, the only
 * fields the engine reads to select a version) and the verbatim single-version manifests under each
 * fixture's {@code http/} were captured from the registry with a throwaway node script:
 * <pre>
 *   const p = await (await fetch(`https://registry.npmjs.org/${name}`,
 *       {headers:{accept:'application/vnd.npm.install-v1+json'}})).json();
 *   const versions = {}; for (const v of Object.keys(p.versions)) versions[v] = {};
 *   writeFileSync(name, JSON.stringify({name:p.name, 'dist-tags':p['dist-tags'], versions}));
 *   writeFileSync(`${name}-${ver}`, await (await fetch(
 *       `https://registry.npmjs.org/${name}/${ver}`)).text());
 * </pre>
 */
class NpmClosureAddLockRegenTest {

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

    // --- byte-exact closure adds (goldens from real npm 11.6.2) -----------

    @Test
    void basicClosureV3() {
        // supports-color -> has-flag: both hoist top-level, no conflict.
        assertClosureByteExact("lock/npm/closure-basic",
                new String[][]{{"supports-color", "7.2.0"}, {"has-flag", "4.0.0"}});
    }

    @Test
    void basicClosureV2() {
        // The same closure into a lockfileVersion 2 lock: packages entries + the legacy `dependencies`
        // tree (leaf transitive minimal, dependent carries `requires`).
        assertClosureByteExact("lock/npm/closure-basic-v2",
                new String[][]{{"supports-color", "7.2.0"}, {"has-flag", "4.0.0"}});
    }

    @Test
    void deepClosureV3() {
        // ansi-styles -> color-convert -> color-name: a three-level closure (also exercises funding).
        assertClosureByteExact("lock/npm/closure-deep",
                new String[][]{{"ansi-styles", "4.3.0"}, {"color-convert", "2.0.1"}, {"color-name", "1.1.4"}});
    }

    @Test
    void dedupSharedTransitive() {
        // ansi-styles pulls color-convert ^2.0.1, already satisfied at 2.0.1 in the lock -> dedup: only
        // node_modules/ansi-styles is inserted, its subtree is not re-walked.
        assertClosureByteExact("lock/npm/closure-dedup",
                new String[][]{{"ansi-styles", "4.3.0"}});
    }

    @Test
    void devClosure() {
        // supports-color added as a devDependency -> the whole fresh closure is "dev": true.
        assertClosureByteExact("lock/npm/closure-dev",
                new String[][]{{"supports-color", "7.2.0"}, {"has-flag", "4.0.0"}});
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, version}} maps to two recorded routes: {@code http/<name>} (packument) and
     * {@code http/<name>-<version>} (manifest).
     */
    private void assertClosureByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            routes.put(REG + pkg[0], resource(dir + "/http/" + pkg[0]));
            routes.put(REG + pkg[0] + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
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
        // {fixture dir, lockfileVersion}. Re-deriving before AND after with a real npm and asserting
        // equality proves the goldens are genuine npm output (npm reproduces `after` from before + edit).
        String[][] fixtures = {
                {"lock/npm/closure-basic", "3"},
                {"lock/npm/closure-basic-v2", "2"},
                {"lock/npm/closure-deep", "3"},
                {"lock/npm/closure-dedup", "3"},
                {"lock/npm/closure-dev", "3"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }

    private void assertNpmReproduces(String pkgResource, String lockResource, String lockfileVersion) throws Exception {
        Path tmp = Files.createTempDirectory("closure-record");
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
        try (InputStream in = NpmClosureAddLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
