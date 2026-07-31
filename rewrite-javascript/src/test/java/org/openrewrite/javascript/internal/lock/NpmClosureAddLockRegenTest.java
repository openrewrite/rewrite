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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for Phase B npm closure adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code package-lock.json}, the recipe's add edit, and recorded registry
 * HTTP — through {@link NativeLockEngine} entirely OFFLINE (a stub {@code HttpSender} serves the captured
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
class NpmClosureAddLockRegenTest extends LockRegenTestSupport {

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

    // --- reverse-dependent nest on add (Phase B I5) ----------------------

    @Test
    void addNestsConflictingTransitiveUnderParent() {
        // The lock already has ms@2.1.3 top-level (a direct dep). Adding debug@2.6.9 (which needs ms@2.0.0)
        // nests ms@2.0.0 under node_modules/debug, leaving the top-level ms@2.1.3 untouched.
        assertClosureByteExact("lock/npm/add-nest",
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}});
    }

    // --- peer dependencies (optional peers skip, non-optional defers) -----

    @Test
    void optionalPeerSkippedV3() {
        // debug's only peer (supports-color) is optional -> not installed; the entry records
        // peerDependenciesMeta verbatim. The regular dep ms hoists top-level.
        assertClosureByteExact("lock/npm/peer-optional",
                new String[][]{{"debug", "4.4.3"}, {"ms", "2.1.3"}});
    }

    @Test
    void optionalPeerSkippedV2() {
        // The same optional-peer add into a lockfileVersion 2 lock: the packages entry records
        // peerDependenciesMeta; the legacy `dependencies` tree stays minimal (no peer fields).
        assertClosureByteExact("lock/npm/peer-optional-v2",
                new String[][]{{"debug", "4.4.3"}, {"ms", "2.1.3"}});
    }

    @Test
    void optionalPeerWithDeclaredMapV3() {
        // @rollup/pluginutils declares a real peerDependencies map (rollup) marked optional -> rollup is
        // skipped, both peerDependencies + peerDependenciesMeta record verbatim (object group, after engines),
        // and the regular closure (scoped @types/estree, estree-walker, picomatch) hoists top-level.
        assertClosureByteExact("lock/npm/peer-optional-map",
                new String[][]{{"@rollup/pluginutils", "5.4.0"}, {"@types/estree", "1.0.9"},
                        {"estree-walker", "2.0.2"}, {"picomatch", "4.0.5"}});
    }

    @Test
    void nonOptionalPeerFailsLoud() {
        // A non-optional peer npm auto-installs (placed top-level with a `peer: true` marker whose
        // reachability propagation needs the full hoisting model) is deferred rather than guessed.
        routes.put(REG + "has-peer", "{\"name\":\"has-peer\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "has-peer/1.0.0",
                "{\"name\":\"has-peer\",\"version\":\"1.0.0\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/has-peer/-/has-peer-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PEER\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}",
                "{\"dependencies\":{}}",
                "{\"lockfileVersion\":3,\"packages\":{\"\":{}}}",
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("non-optional peerDependencies").contains("react");
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, version}} maps to two recorded routes: {@code http/<name>} (packument) and
     * {@code http/<name>-<version>} (manifest).
     */
    private void assertClosureByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            // The registry client URL-encodes a scope slash (@scope/name -> @scope%2Fname); the resource
            // path keeps the literal slash (a nested http/@scope/ directory).
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
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
                {"lock/npm/add-nest", "3"},
                {"lock/npm/peer-optional", "3"},
                {"lock/npm/peer-optional-v2", "2"},
                {"lock/npm/peer-optional-map", "3"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
