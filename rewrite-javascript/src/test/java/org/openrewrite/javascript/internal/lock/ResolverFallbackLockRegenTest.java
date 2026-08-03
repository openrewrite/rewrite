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

import org.junit.jupiter.api.Test;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.resolve.BunResolver;
import org.openrewrite.javascript.internal.lock.resolve.LockResolver;
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.lock.resolve.YarnClassicResolver;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;
import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that the full {@link NativeLockEngine} falls back to the from-scratch {@link LockResolver} tier
 * (ADR 0012) when the surgical patch defers. Each test drives the WHOLE {@code NativeLockEngine.regenerate(...)}
 * entirely OFFLINE on an edit the surgical path cannot reshape, and asserts the returned lock is BYTE-IDENTICAL to
 * the resolver golden under {@code lock/<pm>/resolve-*}.
 * <p>
 * For npm, pnpm, bun and classic yarn the edit introduces a FORK: the pre-edit lock (a clean closure produced by
 * the same resolver) already installs the package at one version, and the edit adds a second, incompatible one. The
 * surgical tier defers ("would fork") and — since a surgical patch can never fork — a byte-exact fork lock proves
 * the resolver produced it. Yarn Berry has no fork resolver fixture yet, so its test triggers the fallback via a
 * whole-manifest reconcile (no pre-edit manifest to scope a surgical patch); the resolver reproduces the whole lock
 * from scratch, checksums included.
 */
class ResolverFallbackLockRegenTest extends LockRegenTestSupport {

    @Test
    void bunForkAddFallsBackToResolver() {
        // pre-edit declares debug@2.6.9 (ms@2.0.0 hoisted); adding ms@2.1.3 forks. The surgical add defers
        // ("bun would fork"); the resolver reproduces the fork from scratch.
        String dir = "lock/bun/resolve-fork";
        routePackages(dir, new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
        String preEdit = "{\"name\":\"resolve-fork\",\"version\":\"1.0.0\",\"dependencies\":{\"debug\":\"2.6.9\"}}";
        String preEditLock = new BunResolver().resolve(request(preEdit));
        assertForkFallback(PackageManager.Bun, dir, preEdit, preEditLock);
    }

    @Test
    void yarnClassicForkAddFallsBackToResolver() {
        // pre-edit declares debug@2.6.9 (ms@2.0.0 block); adding ms@2.1.3 forks. The surgical add defers
        // ("yarn would fork"); the resolver reproduces the two-block fork from scratch.
        String dir = "lock/yarn-classic/resolve-fork";
        routePackages(dir, new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
        String preEdit = "{ \"name\": \"f\", \"version\": \"1.0.0\", \"dependencies\": { \"debug\": \"2.6.9\" } }";
        String preEditLock = new YarnClassicResolver().resolve(request(preEdit));
        assertForkFallback(PackageManager.YarnClassic, dir, preEdit, preEditLock);
    }

    @Test
    void berryWholeManifestReconcileFallsBackToResolver() {
        // No pre-edit manifest to scope a surgical patch: the surgical tier defers (RESOLUTION_REQUIRED) and the
        // resolver reproduces the whole berry lock from scratch, reproducing every checksum from the tarball bytes.
        String dir = "lock/yarn-berry/resolve-clean";
        String[][] pkgs = {{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}};
        routePackages(dir, pkgs);
        routeTarballs(dir, pkgs);
        // The berry resolver reads only __metadata (version + cacheKey) from the existing lock.
        String existingLock = "__metadata:\n  version: 8\n  cacheKey: 10c0\n";
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                resource(dir + "/pkg"), null, existingLock, null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    @Test
    void workspaceStaysDeferredRatherThanTruncated() {
        // The resolver reproduces a single importer; resolving just one manifest of a multi-importer workspace would
        // drop the siblings. The engine must keep such an edit deferred (a Failure), never emit a truncated lock.
        String workspaceLock = "__metadata:\n  version: 8\n  cacheKey: 10c0\n\n" +
                "\"root@workspace:.\":\n  version: 0.0.0-use.local\n  resolution: \"root@workspace:.\"\n" +
                "  languageName: unknown\n  linkType: soft\n\n" +
                "\"member@workspace:packages/member\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"member@workspace:packages/member\"\n  languageName: unknown\n  linkType: soft\n";
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                "{\"name\":\"root\",\"dependencies\":{\"ms\":\"2.1.3\"}}", null, workspaceLock,
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getLockFileContent()).isNull();
    }

    /** Drive the full engine on the fork edit and assert the fallback reproduced {@code dir/after} byte-for-byte. */
    private void assertForkFallback(PackageManager pm, String dir, String preEditManifest, String preEditLock) {
        Result result = NativeLockEngine.regenerate(pm, resource(dir + "/pkg"), preEditManifest, preEditLock,
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    private void routePackages(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
    }

    private void routeTarballs(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            binaryRoutes.put(REG + pkg[0] + "/-/" + pkg[0] + "-" + pkg[1] + ".tgz",
                    bytesResource(dir + "/http/" + pkg[0] + "-" + pkg[1] + ".tgz"));
        }
    }

    /** A resolver request over the stub registry for generating a clean pre-edit lock (no existing lock). */
    private ResolveRequest request(String manifest) {
        NpmRegistryClient client = new NpmRegistryClient(HttpSenderExecutionContextView.view(ctx).getHttpSender());
        NodeRegistries registries = new NodeRegistries(
                new NodeRegistry(null, REG, null, null, null, null, false, null, true, false),
                Collections.emptyMap(), null, null, null);
        return new ResolveRequest(singletonMap("", manifest), null, registries, client);
    }
}
