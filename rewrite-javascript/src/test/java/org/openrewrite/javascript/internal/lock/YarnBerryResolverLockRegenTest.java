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
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.lock.resolve.YarnBerryResolver;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The differential harness for the Yarn Berry {@link YarnBerryResolver} (ADR 0012). Each test replays a fixture —
 * an edited {@code package.json} and recorded registry HTTP (packument, single-version manifest, and the tarball
 * bytes) — through {@code YarnBerryResolver.resolve(...)} entirely OFFLINE, then asserts the lock it produces is
 * BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code yarn install}. The resolver resolves the
 * whole closure from scratch, reproducing every {@code checksum} from the tarball via {@code BerryZipChecksum}, so
 * byte-identity (checksums included) is the whole contract.
 * <p>
 * The golden was produced with yarn 4.5.3 ({@code cacheKey: 10c0}); {@link #recordGoldenWithRealYarn()}
 * re-derives and verifies it via corepack.
 */
class YarnBerryResolverLockRegenTest extends LockRegenTestSupport {

    // A minimal existing lock: the resolver reads only __metadata (version + cacheKey) from it.
    private static final String EXISTING = "__metadata:\n  version: 8\n  cacheKey: 10c0\n";

    @Test
    void cleanClosure() {
        // baddc -> is-odd@3.0.1 (-> is-number ^6.0.0) + ms@2.1.3: a flat berry lock with reproduced checksums.
        assertResolveByteExact("lock/yarn-berry/resolve-clean", "after", EXISTING,
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void satisfiedPeer() {
        // use-sync-external-store peers react (satisfied by the top-level react); its unquoted peerDependencies range.
        assertResolveByteExact("lock/yarn-berry/resolve-peer", "after", EXISTING,
                new String[][]{{"react", "19.0.0"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerWithMeta() {
        // styled-jsx: dependencies + a quoted peerDependencies range + peerDependenciesMeta (scoped + plain keys).
        assertResolveByteExact("lock/yarn-berry/resolve-peer-meta", "after", EXISTING,
                new String[][]{{"react", "19.0.0"}, {"styled-jsx", "5.1.6"}, {"client-only", "0.0.1"}});
    }

    @Test
    void unsupportedCacheKeyFailsLoud() {
        // Only the 10c0 zip format is validated; any other cacheKey cannot be reproduced, so refuse.
        String dir = "lock/yarn-berry/resolve-clean";
        routeAll(dir, new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new YarnBerryResolver().resolve(request(dir, "__metadata:\n  version: 8\n  cacheKey: 9\n")));
    }

    private void assertResolveByteExact(String dir, String golden, String existingLock, String[][] packages) {
        routeAll(dir, packages);
        assertThat(new YarnBerryResolver().resolve(request(dir, existingLock))).isEqualTo(resource(dir + "/" + golden));
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

    private ResolveRequest request(String dir, String existingLock) {
        NpmRegistryClient client = new NpmRegistryClient(HttpSenderExecutionContextView.view(ctx).getHttpSender());
        NodeRegistries registries = new NodeRegistries(
                new NodeRegistry(null, REG, null, null, null, null, false, null, true, false),
                Collections.emptyMap(), null, null, null);
        return new ResolveRequest(singletonMap("", resource(dir + "/pkg")), existingLock, registries, client);
    }

    // --- live re-record / provenance check (disabled: needs corepack + network) ---

    @Test
    @Disabled("live: runs real yarn 4.5.3 via corepack to re-derive and verify the golden")
    void recordGoldenWithRealYarn() throws Exception {
        Path tmp = Files.createTempDirectory("berry-resolve-record");
        try {
            Files.write(tmp.resolve("package.json"),
                    resource("lock/yarn-berry/resolve-clean/pkg").getBytes(StandardCharsets.UTF_8));
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
                throw new IllegalStateException("yarn install timed out");
            }
            String generated = new String(Files.readAllBytes(tmp.resolve("yarn.lock")), StandardCharsets.UTF_8);
            assertThat(generated).isEqualTo(resource("lock/yarn-berry/resolve-clean/after"));
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
